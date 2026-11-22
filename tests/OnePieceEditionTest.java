package fr.manuel.anime;
import java.nio.file.*;import org.json.*;
public class OnePieceEditionTest {
 public static void main(String[] args)throws Exception{
 String html="<h1 id='titreOeuvre'>One Piece </h1>";
 String raw=Catalog.readerTitle(html);if(!raw.equals("One Piece "))throw new AssertionError("Lost significant space: ["+raw+"]");
 JSONObject book=Catalog.book("<script>panneauScan('Scans (couleur)','scan/vf');panneauScan('Scans (noir et blanc)','scan_noir-et-blanc/vf');</script>","/catalogue/one-piece/");if(book.getJSONArray("editions").length()!=2)throw new AssertionError("Missing edition");
 OfflineStore store=new OfflineStore(Files.createTempDirectory("one-piece-regression").toFile());String id=OfflineStore.key("/catalogue/one-piece/scan_noir-et-blanc/vf/","1");JSONObject meta=new JSONObject().put("id",id).put("title","One Piece").put("sourceTitle",raw).put("chapter","1").put("pages",1);store.save(meta);
 ChapterDownloader.run(store,meta,()->false,(url,dest)->{if(!url.contains("/One%20Piece%20/1/1.jpg"))throw new AssertionError(url);OfflineStore.atomic(dest,new byte[]{(byte)255,(byte)216,(byte)255,1});});System.out.println("PASS distinct editions, significant title space, exact download image URL");
 }
}
