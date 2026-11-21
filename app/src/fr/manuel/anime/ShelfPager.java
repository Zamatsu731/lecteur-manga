package fr.manuel.anime;
import java.util.*;import org.json.*;
/** Keeps source pagination separate from pages shown after media-type filtering. */
final class ShelfPager {
 interface Source {JSONObject page(int page)throws Exception;}
 interface Resolver {String type(JSONObject book)throws Exception;}
 final String expected;final Source source;final Resolver resolver;
 final List<JSONObject> pages=new ArrayList<>();final Deque<JSONObject> pending=new ArrayDeque<>();final Set<String> seen=new HashSet<>();
 int sourcePage=1,unknown=0;boolean sourceMore=true;JSONObject last=new JSONObject();
 ShelfPager(String expected,Source source,Resolver resolver){this.expected=expected;this.source=source;this.resolver=resolver;}
 synchronized JSONObject page(int number)throws Exception{
  number=Math.max(1,number);while(pages.size()<number){JSONArray items=new JSONArray();int loaded=0,resolved=0;boolean interrupted=false;long deadline=System.nanoTime()+18000000000L;
   while(items.length()<24){
    if(pending.isEmpty()){
     if(!sourceMore||loaded>=4||(items.length()>0&&System.nanoTime()>deadline))break;
     try{last=source.page(sourcePage);sourcePage++;}catch(Exception e){if(items.length()==0)throw e;interrupted=true;break;}loaded++;sourceMore=last.optBoolean("more");JSONArray batch=last.getJSONArray("items");for(int i=0;i<batch.length();i++){JSONObject b=batch.getJSONObject(i);if(seen.add(b.getString("path")))pending.addLast(b);}
     if(pending.isEmpty()){if(!sourceMore)break;continue;}
    }
    JSONObject b=pending.peekFirst();String kind=b.optString("kind");
    if(kind.isEmpty()){
     if(resolved>=12||(items.length()>0&&System.nanoTime()>deadline))break;
     // A failed request leaves the item queued so retrying cannot silently lose it.
     try{kind=resolver.type(b);}catch(Exception e){if(items.length()==0)throw e;interrupted=true;break;}resolved++;
    }
    pending.removeFirst();if(kind.isEmpty()){unknown++;continue;}b.put("kind",kind);if(kind.equals(expected)||(expected.equals("Manga")&&kind.equals("Anime-Comics")))items.put(b);
   }
   JSONObject result=new JSONObject().put("items",items).put("more",sourceMore||!pending.isEmpty()).put("genres",last.optJSONArray("genres")==null?new JSONArray():last.getJSONArray("genres")).put("genreValues",last.optJSONArray("genreValues")==null?new JSONArray():last.getJSONArray("genreValues")).put("unclassified",unknown).put("interrupted",interrupted);pages.add(result);
   if(!result.getBoolean("more")&&pages.size()<number)return result;
  }
  return pages.get(number-1);
 }
}
