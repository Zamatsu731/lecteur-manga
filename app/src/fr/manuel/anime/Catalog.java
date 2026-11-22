package fr.manuel.anime;
import java.net.URI;
import java.util.*;
import java.util.regex.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.json.*;

final class Catalog {
 static final String BASE="https://anime-sama.to";
 static boolean bookPath(String p){return p!=null&&p.matches("/catalogue/[a-zA-Z0-9_-]+/?");}
 static boolean editionPath(String p){return p!=null&&p.matches("/catalogue/[a-zA-Z0-9_-]+/scan[a-zA-Z0-9_-]*/vf/?");}
 static boolean imageUrl(String url){try{URI u=new URI(url);String h=u.getHost(),p=u.getPath();return "https".equals(u.getScheme())&&h!=null&&p!=null&&((h.equals("anime-sama.to")&&p.startsWith("/s2/scans/")) || ((h.equals("sushiscan.net")||h.endsWith(".sushiscan.net"))&&p.matches("(?i).*\\.(jpg|jpeg|png|webp|avif)$")&&!p.contains("/ads/")&&!p.contains("/pub/")) || ((h.equals("cdn.jsdelivr.net")||h.equals("raw.githubusercontent.com")||h.equals("s4.anilist.co"))&&p.matches("(?i).*\\.(jpg|jpeg|png|webp|avif)$")&&!p.contains("/autres/")));}catch(Exception e){return false;}}
 static JSONObject search(String html)throws Exception{
  Document d=Jsoup.parse(html,BASE);JSONArray items=new JSONArray(),genres=new JSONArray();Set<String> seen=new HashSet<>();
  for(Element e:d.select(".as-carte-oeuvre")){Element a=e.selectFirst("a.as-carte-lien"),t=e.selectFirst(".as-carte-titre"),i=e.selectFirst("img");if(a==null||t==null)continue;String path=new URI(a.absUrl("href")).getPath();if(!bookPath(path)||!seen.add(path))continue;JSONObject x=new JSONObject();x.put("title",t.text());x.put("path",path);String image=i==null?"":i.absUrl("src");x.put("image",imageUrl(image)?image:"");items.put(x);}
  for(Element e:d.select("input[name=\"genre[]\"]"))if(!e.val().isEmpty())genres.put(e.val());
  boolean more=false;for(Element e:d.select("a[href]"))if(e.text().contains("Suivant"))more=true;
  return new JSONObject().put("items",items).put("genres",genres).put("more",more);
 }
 static JSONObject book(String html,String path)throws Exception{
  if(!bookPath(path))throw new Exception("Manga indisponible.");String url=BASE+path+(path.endsWith("/")?"":"/");Document d=Jsoup.parse(html,url);JSONArray editions=new JSONArray(),genres=new JSONArray();Set<String> seen=new HashSet<>();
  for(Element a:d.select("a.oe-panneau-scan")){String p=new URI(a.absUrl("href")).getPath();if(editionPath(p)&&seen.add(p))editions.put(new JSONObject().put("title",a.text()).put("path",p));}
  if(editions.length()==0)for(Element script:d.select("script")){String code=script.data().replaceAll("(?s)/\\*.*?\\*/","").replaceAll("(?m)^\\s*//.*$","");Matcher m=Pattern.compile("panneauScan\\(\\s*([\"'])(.*?)\\1\\s*,\\s*([\"'])(.*?)\\3\\s*\\)").matcher(code);while(m.find()){String p=new URI(url).resolve(m.group(4)).getPath();if(editionPath(p)&&seen.add(p))editions.put(new JSONObject().put("title",m.group(2)).put("path",p));}}
  for(Element e:d.select(".oe-genres .genre-pill"))genres.put(e.text());Element desc=d.selectFirst("#synopsisText");return new JSONObject().put("editions",editions).put("genres",genres).put("description",desc==null?"":desc.text());
 }
 static String readerTitle(String html)throws Exception{Document d=Jsoup.parse(html);d.outputSettings().prettyPrint(false);Element t=d.getElementById("titreOeuvre");if(t==null||t.text().isEmpty())throw new Exception("Lecture indisponible pour cette édition.");return t.html();}
}
