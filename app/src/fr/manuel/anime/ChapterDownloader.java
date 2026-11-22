package fr.manuel.anime;
import java.io.*;import java.net.URLEncoder;import org.json.*;
/** Serial, resumable chapter transfer. Files become visible only after complete image writes. */
final class ChapterDownloader {
 interface Cancellation {boolean cancelled();}
 interface Fetcher {void image(String url,File dest)throws Exception;}
 static void run(OfflineStore store,JSONObject meta,Cancellation cancel,Fetcher fetch)throws Exception{
  String id=meta.getString("id"),title=meta.optString("sourceTitle",meta.getString("title")),chapter=meta.getString("chapter");int total=meta.getInt("pages");
  for(int i=1;i<=total;i++){
   if(cancel.cancelled()||Thread.currentThread().isInterrupted())throw new InterruptedIOException();
   if(store.hasPage(id,i))continue;
   Exception last=null;
   for(int attempt=0;attempt<2;attempt++){
    try{fetch.image(meta.has("pageUrls")?meta.getJSONArray("pageUrls").getString(i-1):Catalog.BASE+"/s2/scans/"+URLEncoder.encode(title,"UTF-8").replace("+","%20")+"/"+URLEncoder.encode(chapter,"UTF-8")+"/"+i+".jpg",store.page(id,i));if(!store.hasPage(id,i))throw new IOException("Image incomplète. Réessaie.");last=null;break;}
    catch(InterruptedIOException e){throw e;}
    catch(Exception e){last=e;}
   }
   if(last!=null)throw last;
  }
  if(cancel.cancelled())throw new InterruptedIOException();
 }
}
