package ru.ilbagmanov.pdfcreator.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.ilbagmanov.pdfcreator.dto.EmailPasswordDto;
import ru.ilbagmanov.pdfcreator.dto.TokenDto;
import ru.ilbagmanov.pdfcreator.model.User;
import ru.ilbagmanov.pdfcreator.redis.service.RedisUsersService;
import ru.ilbagmanov.pdfcreator.repository.UsersRepository;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private Algorithm algorithm;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisUsersService redisUsersService;

    @SneakyThrows
    @Override
    public TokenDto login(EmailPasswordDto emailPassword) {
        User user = usersRepository.findByEmail(emailPassword.getEmail())
                .orElseThrow((Supplier<Throwable>) () -> new UsernameNotFoundException("User not found"));
        if (passwordEncoder.matches(emailPassword.getPassword(), user.getHashPassword())) {
            String token = JWT.create()
                    .withSubject(user.getId().toString())
                    .withClaim("role", user.getRole().toString())
                    .withClaim("state", user.getState().toString())
                    .withClaim("email", user.getEmail())
                    .withClaim("createdAt", LocalDateTime.now().toString())
                    .sign(algorithm);
            redisUsersService.addTokenToUser(user, token);
            return TokenDto.builder()
                    .token(token)
                    .build();
        } else {
            throw new UsernameNotFoundException("Invalid username or password");
        }
    }
}