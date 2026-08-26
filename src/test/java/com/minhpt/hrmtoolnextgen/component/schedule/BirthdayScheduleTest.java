package com.minhpt.hrmtoolnextgen.component.schedule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.dto.user.UserDto;
import com.minhpt.hrmtoolnextgen.dto.user.UserInfoDto;
import com.minhpt.hrmtoolnextgen.service.EmailService;
import com.minhpt.hrmtoolnextgen.service.user.UserBirthdayService;

/**
 * Unit tests for BirthdaySchedule — plain Mockito, no Spring context.
 *
 * R22.2 sendBirthdayEmails identifies users with a birthday today via
 *        userBirthdayService.getUsersWithBirthdayToday(PaginationRequest).
 * R22.3 sendBirthdayEmails calls emailService.sendBirthdayEmail once per user;
 *        a per-user exception is caught, logged, and the loop continues.
 *
 * Name-building: (firstName + " " + lastName).trim() when userInfo is non-null;
 * falls back to user.getEmail() when userInfo is null.
 *
 * Pagination: the schedule iterates pages (batch size 100) until an empty items
 * list is returned; each page is fetched via getUsersWithBirthdayToday.
 */
@ExtendWith(MockitoExtension.class)
class BirthdayScheduleTest {

    @Mock
    private UserBirthdayService userBirthdayService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BirthdaySchedule birthdaySchedule;

    // -------------------------------------------------------------------------
    // R22.2 / R22.3 — N users with birthdays today → emailService called N times
    // -------------------------------------------------------------------------

    @Test
    void sendBirthdayEmails_twoUsersToday_callsEmailServiceForEach() {
        UserDto alice = userWithInfo(1L, "alice@example.com", "Alice", "Smith");
        UserDto bob   = userWithInfo(2L, "bob@example.com",   "Bob",   "Jones");

        // First page returns 2 users; second page returns empty → loop stops.
        when(userBirthdayService.getUsersWithBirthdayToday(any(PaginationRequest.class)))
                .thenReturn(pageOf(0, 1, List.of(alice, bob)))
                .thenReturn(pageOf(0, 1, List.of()));   // empty → hasMore = false

        birthdaySchedule.sendBirthdayEmails();

        verify(emailService, times(1)).sendBirthdayEmail("alice@example.com", "Alice Smith");
        verify(emailService, times(1)).sendBirthdayEmail("bob@example.com",   "Bob Jones");
        verify(emailService, times(2)).sendBirthdayEmail(any(), any());
    }

    // -------------------------------------------------------------------------
    // R22.3 resilience — one user throws, loop continues for remaining users
    // -------------------------------------------------------------------------

    @Test
    void sendBirthdayEmails_oneUserThrows_loopContinuesAndOthersAreNotified() {
        // Only bob is in the batch; put him alone so we can make his send throw
        // and then verify carol (a separate user in the same batch) is still called.
        // Using answer-style: throw on the second invocation, succeed on first and third.
        UserDto alice = userWithInfo(1L, "alice@example.com", "Alice", "Smith");
        UserDto bob   = userWithInfo(2L, "bob@example.com",   "Bob",   "Jones");
        UserDto carol = userWithInfo(3L, "carol@example.com", "Carol", "Lee");

        when(userBirthdayService.getUsersWithBirthdayToday(any(PaginationRequest.class)))
                .thenReturn(pageOf(0, 1, List.of(alice, bob, carol)))
                .thenReturn(pageOf(0, 1, List.of()));

        // Throw when bob's address is the argument; alice and carol succeed explicitly.
        doThrow(new RuntimeException("SMTP fail for bob"))
                .when(emailService).sendBirthdayEmail(eq("bob@example.com"), any());
        doNothing().when(emailService).sendBirthdayEmail(eq("alice@example.com"), any());
        doNothing().when(emailService).sendBirthdayEmail(eq("carol@example.com"), any());

        assertDoesNotThrow(() -> birthdaySchedule.sendBirthdayEmails());

        // All three were attempted; the loop did not stop after bob's failure.
        verify(emailService, times(3)).sendBirthdayEmail(any(), any());
        verify(emailService).sendBirthdayEmail("alice@example.com", "Alice Smith");
        verify(emailService).sendBirthdayEmail("bob@example.com",   "Bob Jones");
        verify(emailService).sendBirthdayEmail("carol@example.com", "Carol Lee");
    }

    // -------------------------------------------------------------------------
    // Zero birthday users → emailService never called
    // -------------------------------------------------------------------------

    @Test
    void sendBirthdayEmails_noUsersToday_emailServiceNeverCalled() {
        when(userBirthdayService.getUsersWithBirthdayToday(any(PaginationRequest.class)))
                .thenReturn(pageOf(0, 1, List.of()));

        birthdaySchedule.sendBirthdayEmails();

        verify(emailService, never()).sendBirthdayEmail(any(), any());
    }

    // -------------------------------------------------------------------------
    // Name-building: null userInfo → falls back to email address
    // -------------------------------------------------------------------------

    @Test
    void sendBirthdayEmails_nullUserInfo_usesEmailAsUserName() {
        UserDto noInfo = new UserDto();
        noInfo.setId(10L);
        noInfo.setEmail("noinfo@example.com");
        noInfo.setUserInfo(null);

        when(userBirthdayService.getUsersWithBirthdayToday(any(PaginationRequest.class)))
                .thenReturn(pageOf(0, 1, List.of(noInfo)))
                .thenReturn(pageOf(0, 1, List.of()));

        birthdaySchedule.sendBirthdayEmails();

        verify(emailService).sendBirthdayEmail("noinfo@example.com", "noinfo@example.com");
    }

    // -------------------------------------------------------------------------
    // Multi-page pagination — second page is fetched and processed
    // -------------------------------------------------------------------------

    @Test
    void sendBirthdayEmails_multiplePages_allUsersNotified() {
        UserDto alice = userWithInfo(1L, "alice@example.com", "Alice", "A");
        UserDto bob   = userWithInfo(2L, "bob@example.com",   "Bob",   "B");

        // Page 0: currentPage=0, totalPages=2 → hasMore = 0 < 1 = true → fetch page 1.
        // Page 1: currentPage=1, totalPages=2 → hasMore = 1 < 1 = false → loop exits.
        when(userBirthdayService.getUsersWithBirthdayToday(any(PaginationRequest.class)))
                .thenReturn(pageOf(0, 2, List.of(alice)))
                .thenReturn(pageOf(1, 2, List.of(bob)));

        birthdaySchedule.sendBirthdayEmails();

        verify(emailService).sendBirthdayEmail("alice@example.com", "Alice A");
        verify(emailService).sendBirthdayEmail("bob@example.com",   "Bob B");
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static UserDto userWithInfo(long id, String email, String first, String last) {
        UserInfoDto info = UserInfoDto.builder()
                .firstName(first)
                .lastName(last)
                .build();
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setEmail(email);
        dto.setUserInfo(info);
        return dto;
    }

    /**
     * Builds a single-page PaginationResponse.
     *
     * @param currentPage zero-based index of this page
     * @param totalPages  total number of pages in the dataset
     * @param items       contents of this page
     */
    private static PaginationResponse<UserDto> pageOf(int currentPage, int totalPages,
                                                       List<UserDto> items) {
        return PaginationResponse.<UserDto>builder()
                .currentPage(currentPage)
                .totalPages(totalPages)
                .pageSize(100)
                .totalElements((long) items.size())
                .numberOfElements(items.size())
                .sortBy("id")
                .direction("ASC")
                .first(currentPage == 0)
                .last(currentPage == totalPages - 1)
                .items(items)
                .build();
    }
}
