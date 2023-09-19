package potato.potatoAPIserver.security.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * 프론트에서 넘겨받는 OAuth2 엑세스토큰
 * @Author 정순원
 * @Since 2023-08-19
 */
@Getter
@Data
@AllArgsConstructor
public class OAuth2AccessToken {

    private String accessToken;

}