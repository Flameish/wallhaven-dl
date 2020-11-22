import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class init {

    public static void main(String[] args) {
        Grabber wallhaven = new Grabber(createParamsFromArgs(args));
        if(wallhaven.useLogin()) {
            wallhaven.login();
        }
        wallhaven.download();
    }

    public static Map<String, List<String>> createParamsFromArgs(String[] args) {
        final Map<String, List<String>> params = new HashMap<>();
        List<String> options = null;
        for (final String a : args) {
            if (a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    return null;
                }

                options = new ArrayList<>();
                params.put(a.substring(1), options);
            } else if (options != null) {
                options.add(a);
            } else {
                System.err.println("Illegal parameter usage");
                return null;
            }
        }
        return params;
    }
}
