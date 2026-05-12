package com.nexevent.nexevent.services;

import com.nexevent.nexevent.domains.dto.request.TicketCheckInReqDTO;
import com.nexevent.nexevent.domains.dto.response.ResTicketCheckInDTO;
import com.nexevent.nexevent.domains.entities.Checkin;
import com.nexevent.nexevent.domains.entities.Ticket;
import com.nexevent.nexevent.domains.entities.User;
import com.nexevent.nexevent.domains.enums.StatusTicket;
import com.nexevent.nexevent.repositories.CheckinRepository;
import com.nexevent.nexevent.repositories.TicketRepository;
import com.nexevent.nexevent.utils.SecurityUtil;
import com.nexevent.nexevent.utils.TicketQrUtil;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CheckinService {
    private final CheckinRepository checkinRepository;
    private final TicketService ticketService;
    private final TicketQrUtil ticketQrUtil;
    private final UserService userService;

    public CheckinService(CheckinRepository checkinRepository,
                          TicketQrUtil ticketQrUtil,
                          TicketService ticketService,
                          UserService userService){
        this.checkinRepository = checkinRepository;
        this.ticketQrUtil = ticketQrUtil;
        this.ticketService = ticketService;
        this.userService = userService;

    }
    public ResTicketCheckInDTO checkinMethod(TicketCheckInReqDTO reqDTO){
        // Giải mã Token
        String ticketId = ticketQrUtil.verifyAndGetTicketId(reqDTO.getQrToken());

        // Xác thực danh tính nhân viên
        String staffEmail = SecurityUtil.getCurrentUserEmail();
        if (staffEmail == null){
            throw new RuntimeException("Access denied, please try again");
        }
        Optional<User> staff = userService.getUserByEmail(staffEmail);
        if (!staff.isPresent()){
            throw new RuntimeException("Access denied, please try again");
        }
        // Kiểm tra vé từ ticketId lấy trong Token
        Optional<Ticket> ticket = ticketService.findById(ticketId);
        if (!ticket.isPresent()){
            throw new RuntimeException("Ticket does not exist");
        }
        if (ticket.get().getStatus() == StatusTicket.USED){
            throw new RuntimeException("Ticket is already used");
        }
        // Tất cả hợp lệ
        //Đánh dấu vé đã sử dụng
        ticket.get().setStatus(StatusTicket.USED);
        ticketService.saveTicket(ticket.get());
        //Tạo 1 record Checkin mới
        //Gán Staff + Gán Ticket + Gán gate
        Checkin checkin = Checkin.builder()
                .staff(staff.get())
                .ticket(ticket.get())
                .gate(reqDTO.getGate())
                .build();
        checkinRepository.save(checkin);
        //Lưu
        return ResTicketCheckInDTO.builder()
                .ticketId(ticketId)
                .eventName(ticket.get().getOrderItem().getTicketType().getEvent().getTitle())
                .ticketTypeName(ticket.get().getOrderItem().getTicketType().getTitle())
                .customerName(ticket.get().getOrderItem().getOrder().getUser().getFullname())
                .customerEmail(ticket.get().getOrderItem().getOrder().getUser().getEmail())
                .checkedInAt(checkin.getCheckinTime())
                .gate(checkin.getGate())
                .staffName(checkin.getStaff().getFullname())
                .build();

    }
}
