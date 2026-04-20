/*
package com.example.moodlog.domain.mood.controller;

import com.example.moodlog.domain.mood.entity.MoodRecord;
import com.example.moodlog.domain.mood.entity.MoodType;
import com.example.moodlog.domain.mood.service.MoodService;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mood")
public class MoodController {

    private final MoodService moodService;
    private final UserService userService;

    // 감정 기록 폼
    @GetMapping("/write")
    public String writeForm(Model model, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        boolean hasTodayRecord = moodService.existsByUserAndDate(user, LocalDate.now());
        model.addAttribute("hasTodayRecord", hasTodayRecord);
        return "mood/write";
    }

    // 감정 기록 저장
    @PostMapping("/write")
    public String write(@RequestParam MoodType mood,
                        @RequestParam(required = false) String memo,
                        @RequestParam(required = false) String tagText,
                        Principal principal) {

        User user = userService.findByEmail((principal.getName()));
        moodService.createMood(user, mood, memo, tagText);

        return "redirect:/";    // 기록 후 메인 페이지
    }

    // 감정 기록 수정 폼
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("moodRecord", moodService.getMood(id, user));
        return "mood/edit";
    }

    // 감정 기록 수정 처리
    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @RequestParam MoodType mood,
                       @RequestParam(required = false) String memo,
                       @RequestParam(required = false) String tagText,
                       Principal principal) {

        User user = userService.findByEmail(principal.getName());
        moodService.updateMood(id, user, mood, memo, tagText);

        return "redirect:/"; // 수정 후 메인
    }

    // 기록 삭제
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        moodService.deleteMood(id, user);

        return "redirect:/"; // 삭제 후 메인
    }

    // 기록 상세 조회
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        model.addAttribute("moodRecord", moodService.getMood(id, user));
        return "mood/view";
    }

    // 특정 기간 기록 조회
    @GetMapping("/list")
    public String list(@RequestParam(required = false) String start,
                       @RequestParam(required = false) String end,
                       Model model,
                       Principal principal) {

        User user = userService.findByEmail(principal.getName());
        LocalDate startDate = start != null ? LocalDate.parse(start) : LocalDate.now().minusDays(7);
        LocalDate endDate = end != null ? LocalDate.parse(end) : LocalDate.now();

        List moodList = moodService.getRecords(user, startDate, endDate);
        model.addAttribute("moodList", moodList);

        return "mood/list";
    }
}
*/
