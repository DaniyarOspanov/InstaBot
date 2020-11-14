package ru.instaBot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class App {
    public static void main(String[] args) throws IOException {

        Properties properties = new Properties();
        properties.load(new FileInputStream("app.properties"));

        Map<Integer, User> users = new HashMap<>();
        TelegramBot bot = new TelegramBot(properties.getProperty("telegram_token"));

        bot.setUpdatesListener(updates -> {
            updates.forEach(System.out::println);

            updates.forEach(update -> {
                Integer userId = update.message().from().id();

                if (!users.containsKey(userId)) {
                    bot.execute(new SendMessage(update.message().chat().id(),
                            "Пришлите логин и пароль в одном предложении через пробел"));
                    users.put(userId, null);

                } else if (users.get(userId) == null && !update.message().text().equals("null")) {

                    String[] loginAndPassword = update.message().text().split(" ");
                    User user = new User(loginAndPassword[0], loginAndPassword[1]);
                    users.put(userId, user);
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
                    post.setTitle(update.message().text());
                    post.setPhoto(new File("./images/" + update.message().photo()[0].fileId() + ".jpg").getPath());
                    users.get(userId).addPost(post);
                }
            });

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }


}

