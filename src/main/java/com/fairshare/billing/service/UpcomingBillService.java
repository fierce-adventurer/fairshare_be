package com.fairshare.billing.service;

import com.fairshare.billing.dto.BillResponse;
import com.fairshare.billing.dto.CreateBillRequest;
import com.fairshare.billing.model.BillAssignee;
import com.fairshare.billing.model.UpcomingBill;
import com.fairshare.billing.repository.BillAssigneeRepository;
import com.fairshare.billing.repository.UpcomingBillRepository;
import com.fairshare.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UpcomingBillService {

    private final UpcomingBillRepository billRepository;
    private final BillAssigneeRepository assigneeRepository;

    public UpcomingBillService(UpcomingBillRepository billRepository, BillAssigneeRepository assigneeRepository) {
        this.billRepository = billRepository;
        this.assigneeRepository = assigneeRepository;
    }

    @Transactional
    public BillResponse createBill(UUID creatorId, CreateBillRequest request) {
        UpcomingBill bill = new UpcomingBill(
                UUID.randomUUID(),
                request.groupId(),
                request.description().trim(),
                request.currency() != null ? request.currency().trim().toUpperCase() : "INR",
                request.amountMinor(),
                request.dueDate(),
                request.recurrence() != null ? request.recurrence().trim().toLowerCase() : "once",
                request.dueDate().isBefore(LocalDate.now()) ? "overdue" : "pending",
                creatorId
        );

        UpcomingBill saved = billRepository.save(bill);

        Set<UUID> assignees = new HashSet<>();
        if (request.assignedUserIds() != null && !request.assignedUserIds().isEmpty()) {
            assignees.addAll(request.assignedUserIds());
        } else {
            assignees.add(creatorId);
        }

        for (UUID userId : assignees) {
            assigneeRepository.save(new BillAssignee(saved.getId(), userId));
        }

        return BillResponse.from(saved, new ArrayList<>(assignees));
    }

    @Transactional(readOnly = true)
    public List<BillResponse> listUserBills(UUID userId) {
        List<UpcomingBill> bills = billRepository.findByAssignedUserId(userId);
        if (bills.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> billIds = bills.stream().map(UpcomingBill::getId).toList();
        Map<UUID, List<UUID>> assigneesByBillId = assigneeRepository.findByBillIdIn(billIds).stream()
                .collect(Collectors.groupingBy(
                        BillAssignee::getBillId,
                        Collectors.mapping(BillAssignee::getUserId, Collectors.toList())
                ));

        return bills.stream()
                .map(b -> BillResponse.from(b, assigneesByBillId.getOrDefault(b.getId(), Collections.emptyList())))
                .toList();
    }

    @Transactional
    public BillResponse markPaid(UUID userId, UUID billId) {
        UpcomingBill bill = findBillById(billId);
        bill.setStatus("paid");
        UpcomingBill saved = billRepository.save(bill);
        return toResponse(saved);
    }

    @Transactional
    public void deleteBill(UUID userId, UUID billId) {
        UpcomingBill bill = findBillById(billId);
        assigneeRepository.deleteByBillId(billId);
        billRepository.delete(bill);
    }

    private BillResponse toResponse(UpcomingBill bill) {
        List<UUID> assignedIds = assigneeRepository.findByBillId(bill.getId()).stream()
                .map(BillAssignee::getUserId)
                .toList();
        return BillResponse.from(bill, assignedIds);
    }

    private UpcomingBill findBillById(UUID id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }
}
