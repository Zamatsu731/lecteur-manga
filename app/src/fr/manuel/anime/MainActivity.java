package fr.manuel.anime;
import android.app.*;import android.os.*;import android.view.*;import android.webkit.*;import android.net.Uri;import android.widget.*;
import java.io.*;import java.net.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;import org.json.*;

public class MainActivity extends Activity {
 WebView web; OfflineStore store; File metadata; volatile boolean destroyed=false;
 ExecutorService pool=Executors.newFixedThreadPool(3);
 ConcurrentHashMap<String,ShelfPager> shelves=new ConcurrentHashMap<>();
 ConcurrentHashMap<String,DownloadService.Task> tasks=DownloadService.tasks;
 final Object sourceRateLock=new Object();long nextSourceRequest=0L;
 volatile String sourceAgent="";
 static final String ORIGIN="https://reader.local";
 @Override public void onCreate(Bundle b){super.onCreate(b);try{store=new OfflineStore(new File(getFilesDir(),"chapters"));metadata=new File(getFilesDir(),"metadata");metadata.mkdirs();}catch(Exception e){new AlertDialog.Builder(this).setMessage("Stockage inaccessible.").setPositiveButton("Fermer",(d,w)->finish()).show();return;}
  web=new WebView(this);setContentView(web);web.setBackgroundColor(0xff10121b);web.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
  WebSettings s=web.getSettings();sourceAgent=s.getUserAgentString();getSharedPreferences("reader",0).edit().putString("agent",sourceAgent).apply();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setSupportZoom(false);s.setBuiltInZoomControls(false);s.setDisplayZoomControls(false);s.setUseWideViewPort(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);android.webkit.CookieManager.getInstance().setAcceptCookie(false);web.addJavascriptInterface(new Bridge(),"Reader");
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){Uri u=r.getUrl();try{
    if("reader.local".equals(u.getHost())){String path=u.getPath();if(path.equals("/"))return new WebResourceResponse("text/html","UTF-8",getAssets().open("index.html"));if(path.equals("/app.js"))return new WebResourceResponse("application/javascript","UTF-8",getAssets().open("app.js"));if(path.equals("/style.css"))return new WebResourceResponse("text/css","UTF-8",getAssets().open("style.css"));String[] parts=path.split("/");if(parts.length==4&&parts[1].equals("offline")){String id=parts[2];if(parts[3].equals("cover")){File f=new File(store.dir(id),"cover.img");if(f.isFile())return new WebResourceResponse("image/jpeg",null,new FileInputStream(f));}else{int n=Integer.parseInt(parts[3]);if(store.hasPage(id,n))return new WebResourceResponse("image/jpeg",null,new FileInputStream(store.page(id,n)));}}return empty();}
    if(Catalog.imageUrl(u.toString())){String h=u.getHost();if(h!=null&&(h.equals("sushiscan.net")||h.endsWith(".sushiscan.net"))){final HttpURLConnection c=connection(u.toString());if(c.getResponseCode()!=200){c.disconnect();return empty();}String mime=c.getContentType();if(mime==null||!mime.startsWith("image/")){c.disconnect();return empty();}return new WebResourceResponse(mime.split(";")[0],null,new FilterInputStream(c.getInputStream()){@Override public void close()throws IOException{try{super.close();}finally{c.disconnect();}}});}return null;}
   }catch(Exception ignored){}return empty();}
  });web.loadUrl(ORIGIN+"/");
 }
 WebResourceResponse empty(){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}

 String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
 HttpURLConnection connection(String url)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setRequestProperty("User-Agent","Mozilla/5.0 (Android) LecteurManga/4.0");String host=new URL(url).getHost();if(host.equals("sushiscan.net")||host.endsWith(".sushiscan.net")){c.setRequestProperty("User-Agent",sourceAgent);String cookies=android.webkit.CookieManager.getInstance().getCookie(url);if(cookies!=null&&!cookies.isEmpty())c.setRequestProperty("Cookie",cookies);}
 c.setRequestProperty("Referer",(host.equals("sushiscan.net")||host.endsWith(".sushiscan.net")?SushiCatalog.BASE:Catalog.BASE)+"/");return c;}
 String get(String url)throws Exception{if(SushiCatalog.host(new URL(url).getHost()))synchronized(sourceRateLock){long now=System.nanoTime(),wait=nextSourceRequest-now;if(wait>0)Thread.sleep((wait+999999L)/1000000L);nextSourceRequest=System.nanoTime()+500000000L;}HttpURLConnection c=connection(url);try{if(c.getResponseCode()!=200)throw new IOException(url.startsWith(SushiCatalog.BASE)?"Le catalogue complémentaire est inaccessible (HTTP "+c.getResponseCode()+"). Réessaie plus tard.":"Le catalogue est momentanément indisponible.");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>12000000)throw new IOException("Réponse trop volumineuse.");}return out.toString("UTF-8");}}finally{c.disconnect();}}
 JSONObject cached(String key)throws Exception{return new JSONObject(new String(Files.readAllBytes(new File(metadata,key+".json").toPath()),StandardCharsets.UTF_8));}
 void cache(String key,JSONObject data){try{OfflineStore.atomic(new File(metadata,key+".json"),data.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}}
 JSONObject chapters(String path,boolean refresh)throws Exception{if(SushiCatalog.path(path))return sushiChapters(path,refresh);if(!Catalog.editionPath(path))throw new IOException("Édition indisponible.");String key=OfflineStore.key(path,"metadata");if(!refresh)try{return cached(key);}catch(Exception ignored){}try{String title=Catalog.readerTitle(get(Catalog.BASE+path));JSONObject ch=new JSONObject(get(Catalog.BASE+"/s2/scans/get_nb_chap_et_img.php?oeuvre="+enc(title)));JSONObject result=new JSONObject().put("title",title).put("path",path).put("chapters",ch);cache(key,result);return result;}catch(Exception e){try{return cached(key);}catch(Exception ignored){throw new IOException("Connexion nécessaire pour charger les chapitres. Tes téléchargements restent accessibles.");}}}

 JSONObject sushiChapters(String path,boolean refresh)throws Exception{String key=OfflineStore.key(path,"metadata");if(!refresh)try{return cached(key);}catch(Exception ignored){}try{JSONObject result=SushiCatalog.chapters(get(SushiCatalog.url(path)),path);cache(key,result);return result;}catch(Exception e){try{return cached(key);}catch(Exception ignored){throw e;}}}
 JSONObject sushiPages(String path,String chapter)throws Exception{JSONObject info=sushiChapters(path,false);String link=info.getJSONObject("links").optString(chapter);if(!SushiCatalog.siteUrl(link))throw new IOException("Chapitre indisponible.");String key=OfflineStore.key(path,chapter+"-pages");try{JSONArray images=SushiCatalog.pages(get(link),link);JSONObject result=new JSONObject().put("images",images).put("pages",images.length());cache(key,result);return result;}catch(Exception e){try{return cached(key);}catch(Exception ignored){throw e;}}}
 JSONObject sushiBook(String path,boolean refresh)throws Exception{
  String key=OfflineStore.key(path,"book");if(!refresh)try{JSONObject r=cached(key);if(r.optInt("kindSchema",0)>=2&&!r.optString("kind").isEmpty())return r;}catch(Exception ignored){}
  JSONObject result=SushiCatalog.book(get(SushiCatalog.url(path)),path);cache(key,result);return result;
 }
 JSONObject sushiSearch(JSONObject a)throws Exception{
  int page=Math.max(1,Math.min(500,a.optInt("page",1)));String expected=a.optString("type").equals("Comics")?"Comics":"Manga",query=a.optString("q"),sort=a.optString("sort");String order=sort.equals("recent")?"update":sort.equals("ajout")?"latest":"title";
  String signature=OfflineStore.key(expected+"|"+query+"|"+order+"|"+String.valueOf(a.optJSONArray("genres")),"shelf-v3");ShelfPager shelf=shelves.get(signature);
  if(page==1||shelf==null){
   JSONObject types;try{types=cached("sushi-types-v3");}catch(Exception e){types=SushiCatalog.typeValues(get(SushiCatalog.BASE+"/catalogue/"));if(types.length()>0)cache("sushi-types-v3",types);}
   final String type=types.optString(expected,expected.equals("Comics")?"Comic":"Manga");JSONObject values=new JSONObject();try{values=cached("sushi-genres");}catch(Exception ignored){}String selected="";JSONArray genres=a.optJSONArray("genres");if(genres!=null)for(int i=0;i<Math.min(5,genres.length());i++){String name=genres.getString(i);if(values.has(name))selected+="&genre%5B%5D="+enc(values.getString(name));}final String genreQuery=selected;
   shelf=new ShelfPager(expected,sourcePage->{String url=SushiCatalog.BASE+(query.isEmpty()?"/catalogue/?page="+sourcePage:"/page/"+sourcePage+"/?s="+enc(query))+"&order="+order+"&type="+enc(type)+genreQuery;JSONObject r=SushiCatalog.search(get(url));JSONArray filters=r.getJSONArray("genreValues");JSONObject mapped=new JSONObject();for(int i=0;i<filters.length();i++){JSONObject v=filters.getJSONObject(i);mapped.put(v.getString("name"),v.getString("value"));}if(mapped.length()>0)cache("sushi-genres",mapped);return r;},book->sushiBook(book.getString("path"),false).optString("kind"));
   if(shelves.size()>12)shelves.clear();shelves.put(signature,shelf);
  }
  JSONObject result=new JSONObject(shelf.page(page).toString());if(a.optBoolean("cumulative",false)&&page>1){JSONArray all=new JSONArray();Set<String> seen=new HashSet<>();for(int n=1;n<=page;n++){JSONArray batch=shelf.page(n).getJSONArray("items");for(int i=0;i<batch.length();i++){JSONObject item=batch.getJSONObject(i);if(seen.add(item.getString("path")))all.put(item);}}result.put("items",all);}return result;
 }
 JSONArray categoryList(String type,boolean refresh)throws Exception{
  if(type.equals("Comics")||type.equals("SushiManga")){
   JSONObject mapping=new JSONObject();if(!refresh)try{mapping=cached("sushi-genres");}catch(Exception ignored){}
   if(mapping.length()==0){JSONArray filters=SushiCatalog.genres(org.jsoup.Jsoup.parse(get(SushiCatalog.BASE+"/catalogue/"),SushiCatalog.BASE));for(int i=0;i<filters.length();i++){JSONObject f=filters.getJSONObject(i);mapping.put(f.getString("name"),f.getString("value"));}if(mapping.length()>0)cache("sushi-genres",mapping);}
   JSONArray names=new JSONArray();Iterator<String> it=mapping.keys();while(it.hasNext())names.put(it.next());return names;
  }
  if(!refresh)try{return cached("categories").getJSONArray("genres");}catch(Exception ignored){}
  JSONArray genres=Catalog.search(get(Catalog.BASE+"/catalogue/?type%5B%5D=Scans")).getJSONArray("genres");if(genres.length()>0)cache("categories",new JSONObject().put("genres",genres));return genres;
 }
 String friendly(Exception e){if(e instanceof SocketTimeoutException)return "Connexion trop lente. Réessaie.";if(e instanceof UnknownHostException||e instanceof ConnectException)return "Pas de connexion. Retrouve tes chapitres dans Téléchargements.";String m=e.getMessage();if(m==null||m.startsWith("https:")||m.contains("anime-sama")||m.length()>180)return "Chargement impossible. Réessaie.";return m;}
 JSONObject downloadedList()throws Exception{JSONArray list=store.list();long bytes=0;for(int i=0;i<list.length();i++){JSONObject m=list.getJSONObject(i);DownloadService.Task t=tasks.get(m.getString("id"));String state=m.getBoolean("complete")?"complete":t==null?"paused":t.state;m.put("state",state);m.remove("pageUrls");if(t!=null)m.put("error",t.error);m.put("cover",new File(store.dir(m.getString("id")),"cover.img").isFile());bytes+=m.optLong("bytes");}return new JSONObject().put("items",list).put("bytes",bytes).put("free",getFilesDir().getUsableSpace());}
 synchronized JSONObject begin(JSONObject a)throws Exception{
  String id=a.optString("id"),chapter=a.optString("chapter"),path=a.optString("path");JSONObject m;
  if(!id.isEmpty())m=store.meta(id);else{boolean sushi=SushiCatalog.path(path);if(!(sushi?chapter.matches("[a-f0-9]{16}"):chapter.matches("[0-9]+(?:\\.[0-9]+)?")))throw new IOException("Chapitre invalide.");JSONObject info=chapters(path,false);JSONObject pageData=sushi?sushiPages(path,chapter):null;int count=sushi?pageData.getInt("pages"):info.getJSONObject("chapters").optInt(chapter,0);if(count<1||count>3000)throw new IOException("Aucune page disponible.");id=OfflineStore.key(path,chapter);try{m=store.meta(id);}catch(Exception e){m=new JSONObject().put("id",id).put("path",path).put("title",info.getString("title")).put("chapter",chapter).put("pages",count).put("book",a.optJSONObject("book")==null?new JSONObject():a.getJSONObject("book")).put("created",System.currentTimeMillis());if(sushi){m.put("pageUrls",pageData.getJSONArray("images")).put("chapterLabel",info.getJSONObject("labels").getString(chapter)).put("source","sushi");JSONArray order=info.getJSONArray("order");for(int i=0;i<order.length();i++)if(order.getString(i).equals(chapter))m.put("chapterOrder",i);}store.save(m);}}
  DownloadService.Task existing=tasks.get(id);if(existing!=null&&(existing.state.equals("queued")||existing.state.equals("downloading")))return new JSONObject().put("id",id);
  if(store.status(id).getBoolean("complete"))return new JSONObject().put("id",id);
  if(getFilesDir().getUsableSpace()<50L*1024*1024)throw new IOException("Espace insuffisant.");
  final String key=id;DownloadService.Task reservation=new DownloadService.Task();DownloadService.Task prior=tasks.putIfAbsent(id,reservation);if(prior!=null){synchronized(prior){if(prior.state.equals("queued")||prior.state.equals("downloading"))return new JSONObject().put("id",id);tasks.put(id,reservation);}}java.util.concurrent.FutureTask<Void> start=new java.util.concurrent.FutureTask<>(()->{if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},33);startForegroundService(new android.content.Intent(this,DownloadService.class).putExtra("id",key));return null;});runOnUiThread(start);try{start.get();}catch(Exception failure){reservation.state="error";reservation.error="Impossible de démarrer en arrière-plan. Réessaie avec l’application ouverte.";throw new IOException(reservation.error);}return new JSONObject().put("id",key);
 }
 Object dispatch(String action,JSONObject a)throws Exception{
  switch(action){
   case "search":{if(a.optString("type").equals("Comics")||a.optString("type").equals("SushiManga"))return sushiSearch(a);int page=Math.max(1,Math.min(300,a.optInt("page",1)));String type=a.optString("type","Scans");if(!type.equals("Scans")&&!type.equals("Webtoon"))type="Scans";String sort=a.optString("sort","titre");if(!Arrays.asList("titre","ajout","recent","ancien","chapitres","pertinence").contains(sort))sort="titre";String url=Catalog.BASE+"/catalogue/?type%5B%5D="+enc(type)+"&search="+enc(a.optString("q"))+"&page="+page+"&tri="+sort;JSONArray genres=a.optJSONArray("genres");if(genres!=null)for(int i=0;i<Math.min(genres.length(),5);i++)url+="&genre%5B%5D="+enc(genres.getString(i));JSONObject result=Catalog.search(get(url));if(result.getJSONArray("genres").length()>0)cache("categories",new JSONObject().put("genres",result.getJSONArray("genres")));return result;}
   case "categories":return categoryList(a.optString("type","Scans"),a.optBoolean("refresh",false));
   case "book":{String path=a.getString("path");if(SushiCatalog.path(path)){String key=OfflineStore.key(path,"book");try{JSONObject result=sushiBook(path,true);return result;}catch(Exception e){try{JSONObject previous=cached(key);if(previous.optInt("kindSchema",0)<2)previous.put("kind","");return previous;}catch(Exception ignored){throw e;}}}if(!Catalog.bookPath(path))throw new IOException("Manga introuvable.");String key=OfflineStore.key(path,"book");try{JSONObject r=Catalog.book(get(Catalog.BASE+path+(path.endsWith("/")?"":"/")),path);cache(key,r);return r;}catch(Exception e){try{return cached(key);}catch(Exception ignored){throw e;}}}
   case "chapters":return chapters(a.getString("path"),a.optBoolean("refresh",true));
   case "pages":return sushiPages(a.getString("path"),a.getString("chapter"));
   case "downloads":return downloadedList();
   case "download":return begin(a);
   case "cancel":{DownloadService.Task t=tasks.get(a.getString("id"));if(t!=null){t.cancel=true;}return true;}
   case "delete":{String id=a.getString("id");DownloadService.Task t=tasks.get(id);if(t!=null&&(t.state.equals("downloading")||t.state.equals("queued")))throw new IOException("Mets le téléchargement en pause avant de le supprimer.");store.remove(id);tasks.remove(id);return true;}
   case "finish":runOnUiThread(()->finish());return true;
   default:throw new IOException("Action indisponible.");
  }
 }
 public class Bridge {
  @JavascriptInterface public void request(String id,String action,String args){if(destroyed)return;pool.execute(()->{JSONObject r=new JSONObject();try{r.put("data",dispatch(action,new JSONObject(args)));}catch(Exception e){try{r.put("error",friendly(e));}catch(Exception ignored){}}String js="receive("+JSONObject.quote(id)+","+r.toString()+")";runOnUiThread(()->{if(!destroyed&&web!=null)web.evaluateJavascript(js,null);});});}
 }
 @Override public void onBackPressed(){if(web!=null)web.evaluateJavascript("goBack()",null);}
 @Override protected void onDestroy(){destroyed=true;pool.shutdownNow();if(web!=null){web.removeJavascriptInterface("Reader");web.destroy();}super.onDestroy();}
}
