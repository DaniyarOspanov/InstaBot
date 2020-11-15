package ru.instaBot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import ru.instaBot.entity.Post;
import ru.instaBot.entity.User;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class App {
    public static void main(String[] args) throws IOException {

        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("instabot");

        Properties properties = new Properties();
        properties.load(new FileInputStream("app.properties"));

        TelegramBot bot = new TelegramBot(properties.getProperty("telegram_token"));

        bot.setUpdatesListener(updates -> {
            updates.forEach(System.out::println);

            updates.forEach(update -> {

                Integer userId = update.message().from().id();
                EntityManager entityManager = entityManagerFactory.createEntityManager();
                entityManager.getTransaction().begin();

                User user = entityManager.find(User.class, userId);

                if (user == null) {
                    bot.execute(new SendMessage(update.message().chat().id(),
                            "Пришлите логин и пароль в одном предложении через пробел"));
                    entityManager.persist(new User(userId, null, null));
                } else if (user.getLogin() == null) {

                    String[] loginAndPassword = update.message().text().split(" ");
                    user.setLogin(loginAndPassword[0]);
                    user.setPassword(loginAndPassword[1]);
                    entityManager.persist(user);

                    bot.execute(new SendMessage(update.message().chat().id(),
                            "Теперь вы можете присылать текст/изображение для " +
                                    "Instagram (в одном сообщении)"));

                } else if (update.message().photo() != null && update.message().photo().length > 0) {

                    GetFileResponse fileResponse = bot.execute(new GetFile(update.message().photo()[0].fileId()));
                    String fullPath = bot.getFullFilePath(fileResponse.file());
                    try {
                        HttpDownload.downloadFile(fullPath, "./images", update.message().photo()[0].fileId() + ".jpg");
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }

                    Post post = new Post();
                    post.setTitle(update.message().caption());
                    post.setPhoto(new File("./images/" + update.message().photo()[0].fileId() + ".jpg").getPath());
                    user.addPost(post);

                    entityManager.persist(post);
                    entityManager.persist(user);
                }
                entityManager.getTransaction().commit();
                entityManager.close();

            });

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

       // entityManagerFactory.close();
    }


}

