package Media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class NSFWUtils {

    public static String get4k(){
        return ImageUtils.getRandomRedditPic(new String[]{"NSFW_Wallpapers", "SexyWallpapers", "HighResNSFW", "nsfw_hd", "UHDnsfw"});
    }

    public static String getAhegao(){
        return ImageUtils.getRandomRedditPic(new String[]{"ahegao"});
    }

    public static String getAmateur(){
        return ImageUtils.getRandomRedditPic(new String[]{"RealGirls", "amateur", "gonewild"});
    }

    public static String getAsian(){
        return ImageUtils.getRandomRedditPic(new String[]{"AsianHotties", "juicyasians", "asianbabes"});
    }

    public static String getAss(){
        Connection con = Jsoup.connect("http://api.obutts.ru/butts/0/1/random/").ignoreContentType(true);
        try{
            Document doc = con.get();
            JsonNode pics = new ObjectMapper().readTree(doc.body().text());
            return "http://media.obutts.ru/" + pics.get(0).get("preview").asText();
        }catch (IOException ex){
            ex.printStackTrace();
            return null;
        }
    }

    public static String getBara(){
        return ImageUtils.getRandomRedditPic(new String[]{"baramanga"});
    }

    public static String getBBW(){
        return ImageUtils.getRandomRedditPic(new String[]{"BBW", "BBWnudists", "BBW_Chubby"});
    }

    public static String getBDSM(){
        return ImageUtils.getRandomRedditPic(new String[]{"bdsm", "bondage"});
    }

    public static String getBoobs(){
        Connection con = Jsoup.connect("http://api.oboobs.ru/boobs/0/1/random/").ignoreContentType(true);
        try {
            Document doc = con.get();
            JsonNode pics = new ObjectMapper().readTree(doc.body().text());
            return "http://media.oboobs.ru/" + pics.get(0).get("preview").asText();
        }catch (IOException ex){
            ex.printStackTrace();
            return null;
        }
    }

    public static String getCosplay(){
        return ImageUtils.getRandomRedditPic(new String[]{"nsfwcosplay", "cosplayonoff", "cosporn", "cosplayboobs"});
    }

    public static String getDick(){
        return "";///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public static String getE621(){
        return "";///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public static String getEcchi(){
        return ImageUtils.getRandomRedditPic(new String[]{"ecchi"});
    }

    public static String getFuta(){
        return ImageUtils.getRandomRedditPic(new String[]{"futanari"});
    }

    public static String getGay(){
        return "";
    }

    public static String getGif(){
        return ImageUtils.getRandomRedditPic(new String[]{"NSFW_GIF", "nsfw_gifs", "porninfifteenseconds", "60FPSPorn", "porn_gifs", "nsfw_Best_Porn_Gif", "LipsThatGrip", "adultgifs"});
    }

    public static String getGrool(){
        return ImageUtils.getRandomRedditPic(new String[]{"grool"});
    }

    public static String getHentai(){
        return ImageUtils.getRandomRedditPic(new String[]{"HENTAI_GIF", "hentai_irl", "hentai", "HQHentai", "rule34"});
    }

    public static String getLesbian(){
        return "";
    }

    public static String getLingerie(){
        return ImageUtils.getRandomRedditPic(new String[]{"lingerie", "stockings", "Pantyfetish", "panties"});
    }

    public static String getMilf(){
        return ImageUtils.getRandomRedditPic(new String[]{"milf", "amateur_milf", "NotTeenNotMilf"});
    }

    public static String getMonsterGirl(){
        return ImageUtils.getRandomRedditPic(new String[]{"MonsterGirl"});
    }

    public static String getNeko(){
        return "";///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public static String getNsfw(){
        return ImageUtils.getRandomRedditPic(new String[]{"nsfw", "porn", "BonerMaterial", "adorableporn", "nsfw2", "NSFW_nospam"});
    }

    public static String getPaizuri(){
        return ImageUtils.getRandomRedditPic(new String[]{"Paizuri"});
    }

    public static String getPantsu(){
        return ImageUtils.getRandomRedditPic(new String[]{"pantsu"});
    }

    public static String getPublic(){
        return ImageUtils.getRandomRedditPic(new String[]{"naughtyinpublic", "gwpublic", "exposedinpublic", "beachgirls"});
    }

    public static String getPussy(){
        return ImageUtils.getRandomRedditPic(new String[]{"pussy", "rearpussy", "simps", "vagina", "MoundofVenus", "PerfectPussies", "spreading"});
    }

    public static String getSexy(){
        return ImageUtils.getRandomRedditPic(new String[]{"SexyButNotPorn"});
    }

    public static String getSnapchat(){
        return ImageUtils.getRandomRedditPic(new String[]{"NSFW_Snapchat", "snapchatgw"});
    }

    public static String getSukebei(){
        return ImageUtils.getRandomRedditPic(new String[]{"Sukebei"});
    }

    public static String getTentacle(){
        return ImageUtils.getRandomRedditPic(new String[]{"Tentai"});
    }

    public static String getTrap(){
        return ImageUtils.getRandomRedditPic(new String[]{"traphentai"});
    }

    public static String getUniform(){
        return ImageUtils.getRandomRedditPic(new String[]{"MilitaryGoneWild", "sexyuniforms"});
    }

    public static String getYaoi(){
        return ImageUtils.getRandomRedditPic(new String[]{"yaoi"});
    }

    public static String getYuri(){
        return ImageUtils.getRandomRedditPic(new String[]{"yuri"});
    }

    public static String getZettaiRyouiki(){
        return ImageUtils.getRandomRedditPic(new String[]{"ZettaiRyouiki"});
    }

}
