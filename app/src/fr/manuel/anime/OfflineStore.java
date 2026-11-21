package fr.manuel.anime;
import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import org.json.*;

final class OfflineStore {
 final File root;
 OfflineStore(File root)throws IOException{this.root=root;if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Stockage inaccessible.");}
 static String key(String path,String chapter)throws Exception{byte[] bytes=MessageDigest.getInstance("SHA-256").digest((path+"#"+chapter).getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 File dir(String id)throws IOException{if(id==null||!id.matches("[a-f0-9]{64}"))throw new IOException("Chapitre invalide.");return new File(root,id);}
 File page(String id,int n)throws IOException{if(n<1||n>3000)throw new IOException("Page invalide.");return new File(dir(id),n+".jpg");}
 synchronized JSONObject meta(String id)throws Exception{return new JSONObject(new String(Files.readAllBytes(new File(dir(id),"meta.json").toPath()),StandardCharsets.UTF_8));}
 synchronized void save(JSONObject m)throws Exception{String id=m.getString("id");File d=dir(id);if(!d.exists()&&!d.mkdirs())throw new IOException("Stockage inaccessible.");atomic(new File(d,"meta.json"),m.toString().getBytes(StandardCharsets.UTF_8));}
 static void atomic(File dest,byte[] bytes)throws IOException{File temp=new File(dest.getPath()+".tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);out.getFD().sync();}move(temp,dest);}
 static void move(File src,File dest)throws IOException{try{Files.move(src.toPath(),dest.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(src.toPath(),dest.toPath(),StandardCopyOption.REPLACE_EXISTING);}}
 static boolean validImage(File f){try(InputStream in=new FileInputStream(f)){byte[] h=new byte[12];int n=in.read(h);return n>=3&&(h[0]==(byte)255&&h[1]==(byte)216&&h[2]==(byte)255 || n>=8&&h[0]==(byte)137&&h[1]==80&&h[2]==78&&h[3]==71 || n==12&&h[0]==82&&h[1]==73&&h[2]==70&&h[3]==70&&h[8]==87&&h[9]==69&&h[10]==66&&h[11]==80);}catch(IOException e){return false;}}
 boolean hasPage(String id,int n)throws Exception{File f=page(id,n);return f.length()>3&&validImage(f);}
 synchronized JSONObject status(String id)throws Exception{JSONObject m=meta(id);int total=m.getInt("pages"),count=0;long bytes=0;for(int i=1;i<=total;i++){File f=page(id,i);if(f.length()>3&&validImage(f)){count++;bytes+=f.length();}}bytes+=new File(dir(id),"cover.img").length();m.put("done",count);m.put("bytes",bytes);m.put("complete",count==total);return m;}
 synchronized JSONArray list()throws Exception{JSONArray a=new JSONArray();File[] dirs=root.listFiles();if(dirs!=null){Arrays.sort(dirs,(x,y)->Long.compare(y.lastModified(),x.lastModified()));for(File d:dirs)if(d.isDirectory())try{a.put(status(d.getName()));}catch(Exception ignored){}}return a;}
 synchronized void remove(String id)throws Exception{delete(dir(id));}
 private void delete(File f)throws IOException{if(f.isDirectory()){File[] a=f.listFiles();if(a!=null)for(File x:a)delete(x);}if(f.exists()&&!f.delete())throw new IOException("Impossible de supprimer le chapitre.");}
}
