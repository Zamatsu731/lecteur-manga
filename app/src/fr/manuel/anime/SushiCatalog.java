package fr.manuel.anime;
import org.jsoup.Jsoup;import org.jsoup.nodes.*;import org.json.*;import java.net.*;import java.util.*;import java.util.regex.*;import java.nio.charset.StandardCharsets;
/** Declarative MangaThemesia HTML parsing; never executes remote scripts. */
final class SushiCatalog {
 static final String BASE="https://sushiscan.net",PREFIX="sushi:";
 static boolean path(String p){return p!=null&&p.matches("sushi:/catalogue/[a-zA-Z0-9_%~-]+/?");}
 static String url(String p)throws Exception{if(!path(p))throw new Exception("Titre indisponible.");return BASE+p.substring(PREFIX.length())+(p.endsWith("/")?"":"/");}
 static boolean host(String h){return h!=null&&(h.equalsIgnoreCase("sushiscan.net")||h.equalsIgnoreCase("www.sushiscan.net"));}
 static String local(String u)throws Exception{URI uri=new URI(u.replace(" ","%20"));if(!"https".equals(uri.getScheme())||!host(uri.getHost())||uri.getUserInfo()!=null||uri.getPort()!=-1)return "";String p=PREFIX+new URI(uri.toASCIIString()).getRawPath();return path(p)?p:"";}
 static boolean siteUrl(String u){try{URI v=new URI(u);String p=v.getPath();return "https".equals(v.getScheme())&&host(v.getHost())&&v.getUserInfo()==null&&v.getPort()==-1&&p!=null&&p.matches("/[a-zA-Z0-9_%~/-]+")&&!p.contains("..")&&!p.contains("wp-admin");}catch(Exception e){return false;}}
 static String image(Element e){if(e==null)return "";for(String a:new String[]{"data-lazy-src","data-src","data-cfsrc","src"}){String s=e.absUrl(a);if(!s.isEmpty()&&!s.startsWith("data:"))return s;}return "";}
 static String kind(String raw){String x=raw==null?"":raw.toLowerCase(Locale.ROOT).replaceAll("[_-]"," ").trim();if(x.matches(".*\\banime\\s*comics?\\b.*"))return "Anime-Comics";if(x.matches(".*\\b(comic|comics)\\b.*"))return "Comics";if(x.matches(".*\\bmanhwa\\b.*"))return "Manhwa";if(x.matches(".*\\bmanhua\\b.*"))return "Manhua";if(x.matches(".*\\bwebtoon\\b.*"))return "Webtoon";if(x.matches(".*\\b(bd|bande dessinee|bande dessinée)\\b.*"))return "BD";if(x.matches(".*\\b(manga|mangas)\\b.*"))return "Manga";return "";}
 static String itemKind(Element e){
  for(Element t:e.select(".typez,.type,[data-type],[title=Comics],[title=Comic],[title=Manga],a[href*=type]")){
   // Labels and classes describe the media type; never use a title or a source as a guess.
   for(String raw:new String[]{t.attr("data-type"),t.attr("title"),t.text()}){String k=kind(raw);if(!k.isEmpty())return k;}
  }
  return "";
 }
 static String detailKind(Document d){
  for(Element row:d.select(".infotable tr")){Element label=row.selectFirst("td,th");if(label!=null&&label.text().replace(":","").trim().matches("(?i)(type|format)")){Element value=row.selectFirst("td:last-child");String k=value==null?"":kind(value.text());if(!k.isEmpty())return k;}}
  for(Element row:d.select(".tsinfo .imptdt,.fmed")){Element value=row.selectFirst("i,span");String label=row.ownText();Element b=row.selectFirst("b");if(b!=null)label=b.text();if(label.replace(":","").trim().matches("(?i)(type|format)")){String k=value==null?"":kind(value.text());if(!k.isEmpty())return k;}}
  for(Element a:d.select(".infox a[href*=type],.seriestucontent a[href*=type],.bigcontent a[href*=type]")){String k=kind(a.text());if(!k.isEmpty())return k;}
  return "";
 }
 static JSONObject typeValues(String html)throws Exception{
  Document d=Jsoup.parse(html,BASE);JSONObject values=new JSONObject();
  for(Element e:d.select("input[name=type],select[name=type] option")){
   String value=e.val();if(value.isEmpty())continue;String label=e.text();
   if(e.tagName().equals("input")){
    String id=e.id();if(!id.isEmpty())for(Element l:d.select("label[for]"))if(l.attr("for").equals(id)){label=l.text();break;}
    if(label.isEmpty()){Element parent=e.parent();if(parent!=null)label=parent.tagName().equals("label")?parent.text():parent.ownText();}
    if(label.isEmpty()){Element next=e.nextElementSibling();if(next!=null&&next.tagName().equals("label"))label=next.text();}
   }
   String k=kind(value);if(k.isEmpty())k=kind(label);if(!k.isEmpty())values.put(k,value);
  }
  return values;
 }
 static JSONObject filter(JSONObject result,String expected)throws Exception{JSONArray old=result.getJSONArray("items"),items=new JSONArray();for(int i=0;i<old.length();i++){JSONObject item=old.getJSONObject(i);String type=item.optString("kind");if(type.equals(expected)||(expected.equals("Manga")&&type.equals("Anime-Comics")))items.put(item);}result.put("items",items);return result;}
 static JSONArray genres(Document d)throws Exception{JSONArray a=new JSONArray();for(Element e:d.select("ul.genrez li")){Element i=e.selectFirst("input[value]"),l=e.selectFirst("label");if(i!=null&&l!=null&&!i.val().isEmpty())a.put(new JSONObject().put("name",l.text()).put("value",i.val()));}return a;}
 static JSONObject search(String html)throws Exception{
  Document d=Jsoup.parse(html,BASE);JSONArray a=new JSONArray(),names=new JSONArray(),filters=genres(d);Set<String> seen=new HashSet<>();
  for(Element e:d.select(".listupd .bsx,.listo .bsx,.listupd .bs,.listo .bs,.utao .uta .imgu,.listupd article,.listo article")){
   Element link=e.selectFirst("a[href*=/catalogue/]");if(link==null)continue;String p;try{p=local(link.absUrl("href"));}catch(Exception ignored){continue;}if(p.isEmpty()||seen.contains(p))continue;
   Element title=e.selectFirst(".tt,h2,h3,.entry-title"),img=e.selectFirst("img");String t=link.attr("title").trim();if(t.isEmpty()&&title!=null)t=title.text();if(t.isEmpty())t=link.text();if(t.isEmpty()&&img!=null)t=img.attr("alt");if(t.trim().isEmpty())continue;
   a.put(new JSONObject().put("title",t.trim()).put("path",p).put("image",image(img)).put("source","sushi").put("kindSchema",2).put("kind",itemKind(e)));seen.add(p);
  }
  for(int i=0;i<filters.length();i++)names.put(filters.getJSONObject(i).getString("name"));boolean more=d.selectFirst("div.pagination .next,div.hpage .r,a.next.page-numbers")!=null;
  if(a.length()==0){String text=d.text().toLowerCase(Locale.ROOT);boolean explicitlyEmpty=text.contains("aucun résultat")||text.contains("aucun manga")||text.contains("no results")||text.contains("nothing found");if(!explicitlyEmpty)throw new Exception("Le catalogue n'a pas pu être lu. Réessaie dans quelques instants.");}
  return new JSONObject().put("items",a).put("genres",names).put("genreValues",filters).put("more",more);
 }
 static JSONObject book(String html,String path)throws Exception{Document d=Jsoup.parse(html,url(path));Element title=d.selectFirst(".entry-title"),desc=d.selectFirst(".entry-content[itemprop=description],.desc");JSONArray tags=new JSONArray(),editions=new JSONArray();for(Element a:d.select(".mgen a,.seriestugenre a,div.gnr a"))tags.put(a.text());editions.put(new JSONObject().put("title","Chapitres et tomes").put("path",path));String mediaKind=detailKind(d);return new JSONObject().put("kindSchema",2).put("kind",mediaKind).put("title",title==null?"":title.text()).put("description",desc==null?"":desc.text()).put("genres",tags).put("editions",editions).put("source","sushi");}
 static JSONObject chapters(String html,String path)throws Exception{Document d=Jsoup.parse(html,url(path));Element t=d.selectFirst(".entry-title");if(t==null)throw new Exception("Liste des tomes inaccessible. La source demande peut-être une vérification.");List<Element> rows=new ArrayList<>(d.select("div.bxcl li, div.cl li, #chapterlist li, ul li:has(div.chbox):has(div.eph-num)"));Collections.reverse(rows);JSONObject counts=new JSONObject(),links=new JSONObject(),labels=new JSONObject();JSONArray order=new JSONArray();Set<String> seen=new HashSet<>();for(Element e:rows){Element a=e.selectFirst(".eph-num a[href],.lch a[href],a[href]");if(a==null)continue;String link=a.absUrl("href");if(!siteUrl(link)||!seen.add(link))continue;Element name=e.selectFirst(".chapternum,.lch a");String label=name==null?a.text():name.text();if(label.isEmpty())continue;String key=OfflineStore.key(link,"chapter").substring(0,16);counts.put(key,1);labels.put(key,label);links.put(key,link);order.put(key);}if(order.length()==0)throw new Exception("Aucun tome ou chapitre accessible pour ce titre.");return new JSONObject().put("source","sushi").put("path",path).put("title",t.text()).put("chapters",counts).put("labels",labels).put("links",links).put("order",order);}
 static JSONArray pages(String html,String url)throws Exception{if(!siteUrl(url))throw new Exception("Lien de lecture invalide.");Document d=Jsoup.parse(html,url);JSONArray pages=new JSONArray();Set<String> seen=new HashSet<>();for(Element e:d.select("#readerarea img")){String u=image(e);if(Catalog.imageUrl(u)&&seen.add(u))pages.put(u);}if(pages.length()>0)return pages;String code=html;for(Element s:d.select("script[src^=data:text/javascript;base64,]")){try{String decoded=new String(Base64.getDecoder().decode(s.attr("src").substring(s.attr("src").indexOf(',')+1)),StandardCharsets.UTF_8);if(decoded.contains("ts_reader.run"))code+="\n"+decoded;}catch(Exception ignored){}}Matcher m=Pattern.compile("[\"']?(?:images|imageUrls)[\"']?\\s*[:=]\\s*(\\[)").matcher(code);while(m.find()){try{Object raw=new JSONTokener(code.substring(m.end()-1)).nextValue();if(!(raw instanceof JSONArray))continue;JSONArray a=(JSONArray)raw;for(int i=0;i<a.length();i++){String u=new URI(url).resolve(a.getString(i)).toString();if(Catalog.imageUrl(u)&&seen.add(u))pages.put(u);}if(pages.length()>0)break;}catch(Exception ignored){}}if(pages.length()==0)throw new Exception("Pages inaccessibles. La source demande une vérification ou utilise un lecteur non pris en charge.");return pages;}
}
