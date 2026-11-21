package fr.manuel.anime;
import org.json.*;
public class AnimeComicRegressionTest{
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception{
  for(String name:new String[]{"Anime-Comics","Anime Comics","ANIME_COMICS","AnimeComic","AnimeComics"})check(SushiCatalog.kind(name).equals("Anime-Comics"),"anime adaptations must stay separate: "+name);
  check(SushiCatalog.kind("Comic").equals("Comics")&&SushiCatalog.kind("Comics").equals("Comics"),"western comic labels supported");
  String form="<select name='type'><option value='Manga'>Manga</option><option value='Comic'>Comics</option><option value='BD'>BD</option><option value='Anime-Comics'>Anime-Comics</option></select>";
  JSONObject values=SushiCatalog.typeValues(form);check(values.getString("Comics").equals("Comic"),"anime option must not overwrite western comic filter");check(values.getString("Anime-Comics").equals("Anime-Comics"),"anime filter keeps its own key");
  String numeric="<div><input name='type' id='a' value='7'><label for='a'>Comics</label><input name='type' id='b' value='8'><label for='b'>Anime-Comics</label></div>";check(SushiCatalog.typeValues(numeric).getString("Comics").equals("7"),"neighbor labels must not overwrite numeric Comic option");
  String[] names={"Dragon Ball Z – Les Films","Le Château dans le ciel","Le Voyage de Chihiro","Mon Voisin Totoro","Princesse Mononoké","Batman","100 Bullets","Watchmen","One Piece"};StringBuilder html=new StringBuilder("<div class='listupd'>");for(int i=0;i<names.length;i++)html.append("<div class='bs'><div class='bsx'><a href='/catalogue/title-").append(i).append("/' title='").append(names[i]).append("'><img src='https://c1.sushiscan.net/a.jpg'></a><span class='type'>").append(i<5?"Anime-Comics":i<8?"Comics":"Manga").append("</span></div></div>");html.append("</div>");JSONObject result=SushiCatalog.search(html.toString());
  ShelfPager comics=new ShelfPager("Comics",p->new JSONObject(result.toString()),b->{throw new AssertionError("Explicit badges do not need guesses");});JSONArray western=comics.page(1).getJSONArray("items");check(western.length()==3,"western shelf must not contain the five anime adaptations");check(western.getJSONObject(0).getString("title").equals("Batman"),"western shelf contains actual Comic type");
  ShelfPager manga=new ShelfPager("Manga",p->new JSONObject(result.toString()),b->"");check(manga.page(1).getJSONArray("items").length()==6,"Japanese anime-comics grouped with manga, not western comics");
  JSONObject detail=SushiCatalog.book("<h1 class='entry-title'>Mon Voisin Totoro</h1><table class='infotable'><tr><td>Type</td><td>Anime-Comics</td></tr></table>","sushi:/catalogue/totoro/");check(detail.getString("kind").equals("Anime-Comics")&&detail.getInt("kindSchema")==2,"detail metadata marked with new classification schema");
  System.out.println("PASS screenshot regression: all five anime adaptations excluded from western Comics; Comic filter not overwritten by Anime-Comics; numeric filter labels isolated; detailed metadata uses new schema");
 }
}
