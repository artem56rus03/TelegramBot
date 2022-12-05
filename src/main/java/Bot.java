import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.DetectFacesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.DetectedFaces;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.Face;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetFileResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Bot extends TelegramLongPollingBot {

    public String getBotUsername() {
        return "Bot";
    }

    public String getBotToken() {
        return "5772948381:AAEpsLz8hJ3Qn5wJM8KexMUgzYFVcJ4MPqA";
    }

    TelegramBot bot;
    TioTelegramBotProperties botProperties;
    TioTelegramBotMessages listMessages;


    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new Bot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        Model model = new Model();
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            switch (message.getText()) {
                case "/start":
                    sendMsg(message, "Привет, добро пожаловать в телеграмм-бот!");
                    break;
                case "/help":
                    sendMsg(message, "Вам нужно чем-то помочь?");
                    break;
                case "/setting":
                    sendMsg(message, "Вы хотите что-то настроить?");
                    break;
                case "Какая сейчас погода в моем городе?":
                    sendMsg(message, "Введите название своего города");
                    break;
                case "/photo":
                    sendMsg(message, "Отправьте фото и узнаю имя человека");
                default:
                    try {
                        sendMsg(message, Weather.getWeather(message.getText(), model));
                    } catch (IOException e) {
                        sendMsg(message, "Извините, данная фукнция недоступна!");
                    }
            }
        }
    }

    public void setButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();

        keyboardFirstRow.add(new KeyboardButton("/start"));
        keyboardFirstRow.add(new KeyboardButton("/help"));
        keyboardFirstRow.add(new KeyboardButton("/setting"));
        keyboardFirstRow.add(new KeyboardButton("/photo"));

        keyboardRowList.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);

    }

    private String getCommand(com.pengrad.telegrambot.model.Update update) {
        String msg = update.message().text();
        if(msg != null && !"".equals(msg) && msg.startsWith("/"))
            return msg.split(" ")[0];
        else if(update.message().photo() != null && update.message().photo().length>0)
            return "PHOTO";
        else
            return "";
    }

    private boolean sendTyping(com.pengrad.telegrambot.model.Message message) throws InterruptedException {
        BaseResponse baseResponse;
        baseResponse = bot.execute(new SendChatAction(message.chat().id(), ChatAction.typing.name()));

        TimeUnit.SECONDS.sleep(1);

        return baseResponse.isOk();
    }

    public void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setChatId(message.getChatId().toString());

        sendMessage.setReplyToMessageId(message.getMessageId());

        sendMessage.setText(text);
        try {

            setButtons(sendMessage);
            sendMessage(sendMessage);

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private File getFile(String fileId) throws Exception {
        GetFile request = new GetFile(fileId);
        GetFileResponse getFileResponse = bot.execute(request);
        File file = getFileResponse.file();
        return file;
    }

    private boolean sendMessage(com.pengrad.telegrambot.model.Message message, String msg) throws InterruptedException {
        boolean ret = true;
        SendResponse sendResponse;
        com.pengrad.telegrambot.request.SendMessage sendMsg = new com.pengrad.telegrambot.request.SendMessage(message.chat().id(), msg);
        sendMsg.parseMode(ParseMode.Markdown);

        if(sendTyping(message)) {
            sendResponse = bot.execute(sendMsg);
            ret = sendResponse.isOk();
        } else {
            ret = false;
        }
        return ret;
    }

    private boolean analyzePhotos(Message message){
        boolean ret = false;
        try {
            PhotoSize photo = (PhotoSize) message.getPhoto();
            File file = getFile(photo.fileId());
            String filePath = "https://api.telegram.org/file/bot" + botProperties.getToken() + "/" + file.filePath();
            System.out.println("Фото: [" + filePath + "]");

            IamOptions options = new IamOptions.Builder()
                    .apiKey(botProperties.getIbmAiDetectFace())
                    .build();

            VisualRecognition visualRecognition = new VisualRecognition("2018-03-19", options);

            DetectFacesOptions detectFacesOptions = new DetectFacesOptions.Builder()
                    .url(filePath)
                    .build();

            DetectedFaces result = visualRecognition.detectFaces(detectFacesOptions).execute();
            System.out.println(result);

            if(result != null && result.getImages().get(0).getFaces().size()>0) {
                for(Face face : result.getImages().get(0).getFaces()) {
                    sendMsg(message, listMessages.faceMessage(face));
                }
            } else {
                sendMsg(message, listMessages.faceNotFound());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ret;
        }
        return ret;
    }
}