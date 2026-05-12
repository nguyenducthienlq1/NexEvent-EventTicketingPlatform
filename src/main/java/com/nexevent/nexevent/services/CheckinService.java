package com.nexevent.nexevent.services;

import com.nexevent.nexevent.repositories.CheckinRepository;
import com.nexevent.nexevent.repositories.TicketRepository;
import com.nexevent.nexevent.utils.TicketQrUtil;
import org.springframework.stereotype.Service;

@Service
public class CheckinService {
    private final CheckinRepository checkinRepository;
    private final TicketRepository ticketRepository;
    private final TicketQrUtil ticketQrUtil;
    public CheckinService(CheckinRepository checkinRepository,
                          TicketQrUtil ticketQrUtil,
                          TicketRepository ticketRepository){
        this.checkinRepository = checkinRepository;
        this.ticketQrUtil = ticketQrUtil;
        this.ticketRepository = ticketRepository;
    }

}
