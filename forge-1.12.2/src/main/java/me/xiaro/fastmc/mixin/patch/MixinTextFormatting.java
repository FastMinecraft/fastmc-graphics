package me.xiaro.fastmc.mixin.patch;

import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;

@Mixin(TextFormatting.class)
public class MixinTextFormatting {
    /**
     * @author Xiaro
     * @reason Regex optimization
     */
    @Overwrite
    @Nullable
    public static String getTextWithoutFormattingCodes(@Nullable String text) {
        if (text == null) {
            return null;
        }
        
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '§') {
                char nextChar = text.charAt(i + 1);
                
                switch (nextChar) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                    case 'l':
                    case 'o':
                    case 'n':
                    case 'm':
                    case 'r':
                        i += 2;
                        continue;
                    default:
                        break;
                }
            }

            stringBuilder.append(c);
        }

        return stringBuilder.toString();
    }
}
