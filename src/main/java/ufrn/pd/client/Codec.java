package ufrn.pd.client;

import java.io.BufferedReader;
import java.io.IOException;

public class Codec {
    public static String decodeHttpMessage(BufferedReader socketReader) throws IOException {
        StringBuilder headerBuilder = new StringBuilder();
        String line;
        int contentLength = 0;
        // Lê os headers até a linha vazia
        while ((line = socketReader.readLine()) != null && !line.isEmpty()) {
            headerBuilder.append(line).append("\n");
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":", 2)[1].trim());
            }
        }
        // Linha vazia que separa header e body
        headerBuilder.append("\n");
        // Lê o corpo se existir
        char[] bodyChars = new char[contentLength];
        int totalRead = 0;
        while (totalRead < contentLength) {
            int read = socketReader.read(bodyChars, totalRead, contentLength - totalRead);
            if (read == -1) break; // fim do stream
            totalRead += read;
        }
        return headerBuilder.toString() + new String(bodyChars, 0, totalRead);
    }
}
