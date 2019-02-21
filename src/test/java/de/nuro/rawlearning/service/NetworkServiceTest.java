package de.nuro.rawlearning.service;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import de.nuro.service.NetworkService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NetworkServiceTest {

    @Autowired
    private NetworkService networkService;

    @Test
    public void guessNumber() throws IOException {

        networkService.guessNumber();
    }

}
