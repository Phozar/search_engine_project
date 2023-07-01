package searchengine.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FalseResponse {
    private boolean result;
    private String error;
}
