package fr.manuel.anime;
import org.json.*;import java.util.*;
public class ShelfPagerTest{
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static JSONObject item(int n,String type)throws Exception{return new JSONObject().put("path","sushi:/catalogue/title-"+n+"/").put("kind",type).put("title","Title "+n);}
 static JSONObject batch(JSONArray items,boolean more)throws Exception{return new JSONObject().put("items",items).put("more",more);}
 public static void main(String[]args)throws Exception{
  Map<String,String> types=new HashMap<>();ShelfPager.Source mixed=page->{JSONArray a=new JSONArray();if(page==1){for(int i=0;i<5;i++)a.put(item(i,"Comics"));for(int i=5;i<13;i++){JSONObject b=item(i,"");types.put(b.getString("path"),i%2==0?"Comics":"Manga");a.put(b);}return batch(a,true);}a.put(item(13,"Comics")).put(item(14,"Comics")).put(item(15,"Manga")).put(item(16,"Manga"));return batch(a,false);};
  ShelfPager comics=new ShelfPager("Comics",mixed,b->types.getOrDefault(b.getString("path"),""));JSONObject comicPage=comics.page(1);check(comicPage.getJSONArray("items").length()==11,"comic shelf finds more than the first five visible badges");check(!comicPage.getBoolean("more"),"all source pages scanned");for(int i=0;i<11;i++)check(comicPage.getJSONArray("items").getJSONObject(i).getString("kind").equals("Comics"),"comic type confirmed");
  ShelfPager manga=new ShelfPager("Manga",mixed,b->types.getOrDefault(b.getString("path"),""));JSONObject mangaPage=manga.page(1);check(mangaPage.getJSONArray("items").length()==6,"comic titles excluded from manga shelf");
  ShelfPager paged=new ShelfPager("Comics",page->{JSONArray a=new JSONArray();for(int i=(page-1)*10;i<page*10;i++)a.put(item(i,"Comics"));return batch(a,page<4);},b->"");JSONArray first=paged.page(1).getJSONArray("items"),second=paged.page(2).getJSONArray("items");check(first.length()==24&&second.length()==16,"pagination refills filtered results");Set<String> all=new HashSet<>();for(JSONArray a:new JSONArray[]{first,second})for(int i=0;i<a.length();i++)check(all.add(a.getJSONObject(i).getString("path")),"no duplicated or skipped source item");check(all.size()==40,"all forty comics available");check(paged.page(1).getJSONArray("items").length()==24,"previous displayed page stable");
  ShelfPager unknown=new ShelfPager("Manga",p->batch(new JSONArray().put(item(1,"")),false),b->"");check(unknown.page(1).getJSONArray("items").length()==0,"unknown type must not become manga");
  final int[] calls={0};ShelfPager flaky=new ShelfPager("Comics",p->{JSONArray a=new JSONArray();return batch(a.put(item(1,"Comics")).put(item(2,"")),false);},b->{if(calls[0]++==0)throw new Exception("temporary failure");return "Comics";});check(flaky.page(1).getJSONArray("items").length()==1,"partial results preserved after network failure");check(flaky.page(2).getJSONArray("items").length()==1,"failed title retained for next request");
  System.out.println("PASS shelf grouping: 11 comics instead of 5, confirmed metadata, no comics in Manga, filtered pagination, no duplicates or skipped items, unknown not guessed, network retry preserves titles");
 }
}
