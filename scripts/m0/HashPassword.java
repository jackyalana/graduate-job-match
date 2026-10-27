import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.springframework.security.crypto.bcrypt.BCrypt;

/** M0 synthetic account bootstrap: password travels through stdin, never argv/log. */
public class HashPassword {
    public static void main(String[] args) throws Exception {
        String password = new BufferedReader(new InputStreamReader(System.in, "UTF-8")).readLine();
        System.out.print(BCrypt.hashpw(password, BCrypt.gensalt()));
    }
}
