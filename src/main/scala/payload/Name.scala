package payload
import org.jboss.netty.buffer.ChannelBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.List

object Name {
  
  /**
	 * 4.1.4. Message compression
	 */
	val MASK_POINTER = 0xC0; // 1100 0000

	/**
	 * 2.3.4. Size limits
	 */
	val MAX_LABEL_SIZE = 63; // 0011 1111

	/**
	 * 2.3.4. Size limits
	 */
	val MAX_NAME_SIZE = 255;

  // we shoudl rewrite this in a more functional way
  def parse(buf: ChannelBuffer) = {    
    val list = ListBuffer.empty[List[Byte]]
    
	var namesize = 0	
	var length = buf.readUnsignedByte
	var jumped = false
	
	while(-1 < length) {
	  
	  if (length == 0) {
				list += List.empty[Byte]
				
			} else if ((length & MASK_POINTER) != 0) {
				val p = ((length ^ MASK_POINTER) << 8) + buf.readUnsignedByte();
				if (jumped == false) {
					buf.markReaderIndex()
					jumped = true
				}
				buf.readerIndex(p);
			} else if (length <= MAX_LABEL_SIZE) {
				namesize += length;
				if (MAX_NAME_SIZE < namesize) {
				  // throw an exception where the name must be 255 or less					
				}
				val marray = new Array[Byte](length)
				buf.readBytes(marray);
				list += marray.toList
			} else {
				// throw an exception because the compression is wrong !
			}
	  
	  		length = buf.readUnsignedByte
		}		

		if (jumped) {
			buf.resetReaderIndex();
		}

		list.toList    
  }
  

}