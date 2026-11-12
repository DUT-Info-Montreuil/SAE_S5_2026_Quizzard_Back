package fr.iutmontreuil.quizzard;

import org.springframework.boot.SpringApplication;

public class TestSaeS52026QuizzardBackApplication {

    public static void main(String[] args) {
        SpringApplication.from(SaeS52026QuizzardBackApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
