package ru.mirea.repair.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mirea.repair.entity.*;
import ru.mirea.repair.repository.RepairRequestRepository;
import ru.mirea.repair.repository.UserRepository;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               RepairRequestRepository requestRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            User user = userRepository.findByEmail("user@example.com").orElseGet(() -> {
                User created = new User();
                created.setEmail("user@example.com");
                created.setFullName("Иван Петров");
                created.setPasswordHash(passwordEncoder.encode("user12345"));
                created.setRole(Role.USER);
                return userRepository.save(created);
            });

            userRepository.findByEmail("admin@example.com").orElseGet(() -> {
                User admin = new User();
                admin.setEmail("admin@example.com");
                admin.setFullName("Администратор системы");
                admin.setPasswordHash(passwordEncoder.encode("admin12345"));
                admin.setRole(Role.ADMIN);
                return userRepository.save(admin);
            });

            if (requestRepository.count() == 0) {
                RepairRequest first = new RepairRequest();
                first.setTitle("Не включается ноутбук");
                first.setDescription("После обновления система не загружается, требуется диагностика.");
                first.setEquipmentType("Ноутбук");
                first.setLocation("Кабинет 204");
                first.setPriority(RequestPriority.HIGH);
                first.setStatus(RequestStatus.NEW);
                first.setUser(user);
                requestRepository.save(first);

                RepairRequest second = new RepairRequest();
                second.setTitle("Принтер печатает с полосами");
                second.setDescription("При печати документов появляются вертикальные полосы.");
                second.setEquipmentType("Принтер");
                second.setLocation("Бухгалтерия");
                second.setPriority(RequestPriority.MEDIUM);
                second.setStatus(RequestStatus.IN_PROGRESS);
                second.setUser(user);
                requestRepository.save(second);
            }
        };
    }
}
