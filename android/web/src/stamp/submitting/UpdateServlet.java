package stamp.submitting;
 
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import javax.servlet.annotation.WebServlet;
import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession; 

@WebServlet(name = "WebSockerServlet", urlPatterns = {"/pushupdates"})
public class UpdateServlet extends WebSocketServlet
{
    private static final long serialVersionUID = 1L;
    private static ArrayList<Updater> mmiList = new ArrayList();
 
    public StreamInbound createWebSocketInbound(String protocol, HttpServletRequest request){
        Updater mmib = new Updater();
		HttpSession session = request.getSession();
		StampRunner runner = (StampRunner) session.getAttribute("runner");
		if(runner == null){
			String stampDir = getServletContext().getInitParameter("stamp.dir");
			runner = new StampRunner(stampDir);
			session.setAttribute("runner", runner);
			runner.start();
		}
		runner.setUpdater(mmib);
		return mmib;
    }
 
	class Updater extends MessageInbound{
        WsOutbound myoutbound;
 
        @Override
		public void onOpen(WsOutbound outbound)
		{
            try {
                System.out.println("Open Client.");
                this.myoutbound = outbound;
                mmiList.add(Updater.this);
                outbound.writeTextMessage(CharBuffer.wrap("Hello!"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
 
        @Override
		public void onClose(int status)
		{
            System.out.println("Close Client.");
            mmiList.remove(Updater.this);
        }
 
        @Override
		public void onTextMessage(CharBuffer cb) throws IOException
		{
        }
		
		@Override
		public void onBinaryMessage(ByteBuffer cb) throws IOException
		{
        }

		public void update(String text) throws IOException
		{
			System.out.println("UPDATE: "+text);
			for(Updater mmib: mmiList){
                CharBuffer buffer = CharBuffer.wrap(text);
                mmib.myoutbound.writeTextMessage(buffer);
                mmib.myoutbound.flush();
            }
		}
    }
}