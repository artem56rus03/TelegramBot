import com.ibm.watson.developer_cloud.visual_recognition.v3.model.Face;
import java.text.DecimalFormat;

public class TioTelegramBotMessages {
    public String faceMessage(Face face) {
        StringBuilder faceMessage = new StringBuilder();
        DecimalFormat df = new DecimalFormat("###.##");
        StringBuilder append = faceMessage.append("Имя известного человека: ").append(getName());
        return faceMessage.toString();
    }

    private String getName() {
        String o = null;
        return o;
    }

    public String faceNotFound() {
        return "Не можем найти никакого лица.\n" +
                "Пожалуйста, пришлите мне еще одну фотографию.";
    }
}