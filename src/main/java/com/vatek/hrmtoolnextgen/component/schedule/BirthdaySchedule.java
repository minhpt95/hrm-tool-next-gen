package com.vatek.hrmtoolnextgen.component.schedule;

import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.service.EmailService;
import com.vatek.hrmtoolnextgen.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.vatek.hrmtoolnextgen.dto.request.PaginationRequest;
import com.vatek.hrmtoolnextgen.dto.response.PaginationResponse;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class BirthdaySchedule {

    private final UserService userService;
    private final EmailService emailService;

    /**
     * Send birthday emails to all users who have birthday today
     * Runs every day at 7:30 AM GMT+7 (Asia/Ho_Chi_Minh timezone)
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "0 30 7 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void sendBirthdayEmails() {
        log.info("Starting birthday email schedule at {}", LocalDate.now());
        
        try {
            int page = 0;
            int pageSize = 100; // Process in batches
            boolean hasMore = true;
            int totalSent = 0;

            while (hasMore) {
                PaginationRequest paginationRequest = 
                    PaginationRequest.builder()
                        .page(page)
                        .size(pageSize)
                        .build();

                PaginationResponse<UserDto> response = 
                    userService.getUsersWithBirthdayToday(paginationRequest);

                List<UserDto> users = response.getItems();
                
                if (users.isEmpty()) {
                    hasMore = false;
                } else {
                    for (UserDto user : users) {
                        try {
                            String userName = user.getUserInfo() != null 
                                ? (user.getUserInfo().getFirstName() + " " + user.getUserInfo().getLastName()).trim()
                                : user.getEmail();
                            
                            emailService.sendBirthdayEmail(user.getEmail(), userName);
                            totalSent++;
                            log.debug("Birthday email sent to: {} ({})", user.getEmail(), userName);
                        } catch (Exception e) {
                            log.error("Failed to send birthday email to: {}", user.getEmail(), e);
                        }
                    }
                    
                    // Check if there are more pages
                    hasMore = response.getCurrentPage() < response.getTotalPages() - 1;
                    page++;
                }
            }

            log.info("Birthday email schedule completed. Total emails sent: {}", totalSent);
        } catch (Exception e) {
            log.error("Error in birthday email schedule", e);
        }
    }
}
