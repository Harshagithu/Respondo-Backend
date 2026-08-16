package com.respondo.seeder;

import com.respondo.entity.IncidentCategory;
import com.respondo.entity.User;
import com.respondo.enums.Role;
import com.respondo.repository.IncidentCategoryRepository;
import com.respondo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds startup data (Section 27): the single ADMIN account, and the
 * default incident categories the citizen incident-report form needs
 * (Phase 3). Team seeding joins this class once ResponderTeam has a
 * workflow to seed for (Phase 7).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@respondo.com";
    private static final String ADMIN_DEFAULT_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedCategories();
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

    private IncidentCategory category(String name, String description, int severityWeight) {
        return IncidentCategory.builder()
                .name(name)
                .description(description)
                .severityWeight(severityWeight)
                .build();
    }
}
