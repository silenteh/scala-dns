package frames;


import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import payload.Message;

public class DnsMessageDecoder extends FrameDecoder {

	   protected Message decode(ChannelHandlerContext ctx,
	                           Channel channel,
	                           ChannelBuffer buf) throws Exception {

	     // Make sure if the length field was received.
	     if (buf.readableBytes() < 4) {
	        // The length field was not received yet - return null.
	        // This method will be invoked again when more packets are
	        // received and appended to the buffer.
	        return null;
	     }

	     // The length field is in the buffer.

	     // Mark the current buffer position before reading the length field
	     // because the whole frame might not be in the buffer yet.
	     // We will reset the buffer position to the marked position if
	     // there's not enough bytes in the buffer.
	     buf.markReaderIndex();

	     // Read the length field.
	     int length = buf.readInt();

	     // Make sure if there's enough bytes in the buffer.
	     if (buf.readableBytes() < length) {
	        // The whole bytes were not received yet - return null.
	        // This method will be invoked again when more packets are
	        // received and appended to the buffer.

	        // Reset to the marked position to read the length field again
	        // next time.
	        buf.resetReaderIndex();

	        return null;
	     }

	     // There's enough bytes in the buffer. Read it.
	     //ChannelBuffer frame = buf.readBytes(length);

	     // Successfully decoded a frame.  Return the decoded frame.
	     return null;
	   }

}
