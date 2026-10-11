package com.healthcheckassistant.admin;

import com.healthcheckassistant.admin.dto.CreatePatientRequest;
import com.healthcheckassistant.admin.dto.UpdatePatientRequest;
import com.healthcheckassistant.common.error.ConflictException;
import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.common.error.InvalidPatientDataException;
import com.healthcheckassistant.patient.PatientBasicData;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import com.healthcheckassistant.admin.dto.*;

@Service
public class PatientManagementService {
    private final PatientManagementRepository repository;
    public PatientManagementService(PatientManagementRepository repository) { this.repository = repository; }
    public List<PatientBasicData> list() { return repository.findAll(); }
    public PatientDetailResponse get(String id) {
        return detail(repository.findById(id).orElseThrow(() -> new NotFoundException("找不到受檢者")));
    }
    @Transactional
    public PatientDetailResponse create(CreatePatientRequest request) {
        String id = clean(request.patientId(), "病歷號");
        if (id.contains("/") || id.contains("\\") || id.contains(";") || id.equals(".") || id.equals(".."))
            throw new InvalidPatientDataException("病歷號不可為路徑分隔字元或相對路徑");
        String name = clean(request.patientName(), "姓名");
        checkBytes(id, 20, "病歷號"); checkBytes(name, 100, "姓名");
        if (repository.findById(id).isPresent()) throw new ConflictException("病歷號已存在");
        var examIds = checkedIds(request.examIds());
        validateArrangement(examIds);
        try {
            requireOne(repository.insert(id, name));
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("病歷號已存在");
        }
        try {
            for (String examId : examIds) requireOne(repository.insertAssignment(id, examId));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("健檢項目安排發生資料關聯衝突，請重新查詢");
        }
        return detail(new PatientBasicData(id, name));
    }
    @Transactional
    public PatientDetailResponse update(String id, UpdatePatientRequest request) {
        String name = clean(request.patientName(), "姓名");
        checkBytes(name, 100, "姓名");
        repository.lockPatient(id).orElseThrow(() -> new NotFoundException("找不到受檢者"));
        if (request.examIds() == null) {
            requireOne(repository.updateName(id, name));
            return detail(new PatientBasicData(id, name));
        }
        repository.lockAssignments(id);
        var current = repository.findAssignedExams(id);
        var target = checkedIds(request.examIds());
        validateArrangement(target);
        var currentIds = new HashSet<String>();
        for (var exam : current) {
            currentIds.add(exam.examId());
            if (!target.contains(exam.examId()) && !exam.removable()) throw new ConflictException(exam.removalBlockReason());
        }
        try {
            requireOne(repository.updateName(id, name));
            for (var exam : current) if (!target.contains(exam.examId())) {
                if (repository.removePendingAssignment(id, exam.examId()) != 1)
                    throw new ConflictException("項目狀態或確認碼已變更，無法移除，請重新查詢");
            }
            for (String examId : target) if (!currentIds.contains(examId)) requireOne(repository.insertAssignment(id, examId));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("健檢項目安排發生資料關聯衝突，請重新查詢");
        }
        return detail(new PatientBasicData(id, name));
    }
    public List<ExamCatalogItem> catalog() {
        var required = new HashMap<String, List<String>>();
        for (var link : repository.findPrerequisites()) required.computeIfAbsent(link.examId(), key -> new ArrayList<>()).add(link.requiredExamId());
        return repository.findExamOptions().stream().map(e -> new ExamCatalogItem(e.examId(), e.examName(), e.stationId(), e.stationName(),
                e.startTime(), e.endTime(), List.copyOf(required.getOrDefault(e.examId(), List.of())))).toList();
    }
    private PatientDetailResponse detail(PatientBasicData patient) {
        return new PatientDetailResponse(patient.patientId(), patient.patientName(), repository.findAssignedExams(patient.patientId()));
    }
    private Set<String> checkedIds(List<String> ids) {
        Set<String> result = new TreeSet<>();
        if (ids != null) for (String id : ids) {
            if (id == null || id.isBlank() || !id.equals(id.strip()) || !result.add(id))
                throw new InvalidPatientDataException("健檢項目代碼不可空白或重複");
        }
        return result;
    }
    private void validateArrangement(Set<String> selected) {
        if (selected.isEmpty()) return;
        Map<String, ExamCatalogItem> options = new HashMap<>();
        for (var item : catalog()) options.put(item.examId(), item);
        Set<String> stations = new HashSet<>();
        for (String id : selected) {
            var item = options.get(id);
            if (item == null) throw new InvalidPatientDataException("無效的健檢項目：" + id);
            if (!stations.add(item.stationId())) throw new ConflictException("同一受檢者在同一站點只能安排一項檢查");
            if (!selected.containsAll(item.requiredExamIds())) throw new ConflictException("健檢項目 " + id + " 缺少前置檢查，請一併安排");
        }
        Map<String, Integer> visit = new HashMap<>();
        for (String id : selected) checkCycle(id, options, visit);
    }
    private void checkCycle(String id, Map<String, ExamCatalogItem> options, Map<String, Integer> visit) {
        if (visit.getOrDefault(id, 0) == 1) throw new ConflictException("健檢前置條件存在循環，無法安排");
        if (visit.getOrDefault(id, 0) == 2) return;
        visit.put(id, 1);
        for (String required : options.get(id).requiredExamIds()) checkCycle(required, options, visit);
        visit.put(id, 2);
    }
    public void delete(String id) {
        int count;
        try {
            count = repository.deleteUnreferenced(id);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("受檢者已有關聯檢查紀錄或確認碼，無法刪除");
        }
        if (count == 0) {
            get(id);
            throw new ConflictException("受檢者已有關聯檢查紀錄或確認碼，無法刪除");
        }
        requireOne(count);
    }
    private String clean(String value, String field) {
        if (value == null || value.isBlank() || value.codePoints().anyMatch(Character::isISOControl))
            throw new InvalidPatientDataException(field + "不可空白或包含控制字元");
        return value.strip();
    }
    private void checkBytes(String value, int limit, String field) {
        if (repository.byteLength(value) > limit) throw new InvalidPatientDataException(field + "不可超過 " + limit + " bytes");
    }
    private void requireOne(int count) {
        if (count != 1) throw new IllegalStateException("Patient write did not affect exactly one row");
    }
}
