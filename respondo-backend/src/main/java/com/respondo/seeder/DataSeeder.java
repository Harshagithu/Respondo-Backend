package com.respondo.seeder;

import com.respondo.entity.IncidentCategory;
import com.respondo.entity.ResponderTeam;
import com.respondo.entity.User;
import com.respondo.enums.Role;
import com.respondo.repository.IncidentCategoryRepository;
import com.respondo.repository.ResponderTeamRepository;
import com.respondo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds startup data (Section 27): the ADMIN account, default incident
 * categories (Phase 3), and — as of Phase 7 — a couple of optional
 * initial responder teams so admin has something to assign responders
 * to without needing to create one by hand first.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@respondo.com";
    private static final String ADMIN_DEFAULT_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final ResponderTeamRepository responderTeamRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedCategories();
        seedTeams();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }

        User admin = User.builder()
                .fullName("System Administrator")
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD))
                .role(Role.ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);
        log.info("Seeded default admin account: {}", ADMIN_EMAIL);
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        // severityWeight feeds PriorityCalculationService (Phase 4) as the
        // "category severity" factor from Section 9.
        List<IncidentCategory> categories = List.of(
                category("Fire", "Structure, vehicle, or wildfire", 8),
                category("Medical Emergency", "Life-threatening injury or illness", 9),
                category("Crime in Progress", "Active criminal activity", 7),
                category("Traffic Accident", "Vehicle collision or road hazard", 5),
                category("Natural Disaster", "Flood, earthquake, storm damage, etc.", 10),
                category("Public Hazard", "Gas leak, downed power line, structural risk", 6),
                category("Other", "Anything not covered by the categories above", 2)
        );

        categoryRepository.saveAll(categories);
        log.info("Seeded {} default incident categories", categories.size());
    }

    private void seedTeams() {
        if (responderTeamRepository.count() > 0) {
            return;
        }

        List<ResponderTeam> teams = List.of(
                team("Alpha Team", "Primary field response unit"),
                team("Bravo Team", "Secondary field response unit")
        );

        responderTeamRepository.saveAll(teams);
        log.info("Seeded {} default responder teams", teams.size());
    }

    private IncidentCategory category(String name, String description, int severityWeight) {
        return IncidentCategory.builder()
                .name(name)
                .description(description)
                .severityWeight(severityWeight)
                .build();
    }

    private ResponderTeam team(String name, String description) {
        return ResponderTeam.builder()
                .name(name)
                .description(description)
                .build();
    }
}
