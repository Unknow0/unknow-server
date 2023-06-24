/**
 * 
 */
package unknow.server.util.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author unknow
 */
public class BuffersUtilsTest {
	private static final String lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque ac sem libero. Proin consectetur id nulla id lobortis. Duis lacus odio, fringilla in nulla et, blandit elementum velit. Nam lacinia blandit purus a tempus. Morbi nisl lectus, fringilla nec eleifend in, sollicitudin ac purus. Mauris sagittis dui non enim sagittis tincidunt vitae et diam. Pellentesque ac condimentum massa. In elementum libero eros, tincidunt scelerisque mauris vulputate id."
			+ "\nMauris commodo turpis volutpat aliquam commodo. Vivamus non aliquet elit. Proin fringilla facilisis hendrerit. In consequat risus iaculis auctor sodales. Sed sit amet arcu libero. Nam efficitur dolor elementum ultrices eleifend. Sed porta, sapien at porttitor euismod, nibh nunc fermentum odio, eu viverra dolor arcu id libero. Curabitur mi dolor, placerat nec ante eu, ultricies aliquet felis. Praesent gravida laoreet feugiat. Nulla sit amet dui sed velit suscipit pharetra in at nibh."
			+ "\nDonec hendrerit, ex ac sodales efficitur, ante mauris egestas nisi, nec cursus nisl leo vel arcu. Pellentesque nisl massa, rutrum non urna id, tempus fermentum mauris. Phasellus non purus in justo finibus tempus ac quis massa. Proin sem libero, commodo et faucibus non, placerat id nunc. Etiam vestibulum porttitor euismod. Etiam eget feugiat justo. Fusce consectetur erat libero, sit amet auctor arcu aliquet sed. Suspendisse potenti. Duis nulla tortor, consectetur vestibulum facilisis ac, porttitor non libero. Suspendisse ac facilisis est, id fermentum sapien. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus."
			+ "\nPhasellus eleifend ipsum magna, ac posuere metus scelerisque quis. Suspendisse elementum tempus feugiat. Praesent ut felis dignissim risus varius bibendum sit amet sed mi. Curabitur egestas tristique accumsan. In hac habitasse platea dictumst. Nulla facilisi. Nunc mattis magna velit, a imperdiet felis elementum et. In tincidunt tellus sed lorem aliquet interdum. Proin eu commodo arcu."
			+ "\nDuis nec nibh vel diam rhoncus pellentesque. Maecenas dapibus purus et libero dapibus, sit amet efficitur neque euismod. Proin a convallis erat. Morbi interdum eu augue eu varius. Proin consequat gravida dolor, eget iaculis magna dapibus vitae. Aliquam neque orci, rhoncus at nisl vitae, eleifend viverra mauris. Sed dignissim urna ut vehicula efficitur. Integer dapibus magna at neque dictum, tempus pellentesque sem ultricies. Sed et urna fringilla, placerat ligula sit amet, aliquam orci. Vestibulum leo velit, iaculis id condimentum a, eleifend eu felis."
			+ "\nVivamus eget magna in enim aliquet tempor. Etiam mauris odio, gravida quis commodo non, aliquet lobortis dui. Vestibulum vitae cursus odio, at malesuada purus. Phasellus vitae neque mattis, blandit arcu tristique, volutpat nisl. Aliquam ac suscipit lectus. Pellentesque rhoncus volutpat augue. Duis sagittis ligula vel neque blandit tempus vel vitae arcu. In vel neque ullamcorper, gravida purus vitae, porta augue. Nullam aliquam felis nec urna porttitor, sed consectetur leo dignissim. Integer luctus mattis tempus. Pellentesque in imperdiet quam, at porttitor neque. Nam mattis lacus sit amet facilisis cursus. Cras sollicitudin metus non eros molestie, nec convallis nisl bibendum. Nam sem magna, tempor sit amet condimentum nec, ultrices eu velit."
			+ "\nFusce ut elit sed nulla condimentum tincidunt. Vestibulum consequat quam orci, et elementum elit mattis vitae. Phasellus ut metus elementum tortor ultrices tempor vel eu arcu. Maecenas sit amet ipsum a risus volutpat sagittis in vel dui. Donec orci quam, sollicitudin et facilisis at, hendrerit non arcu. Proin ultricies laoreet posuere. Quisque pharetra, massa in egestas ornare, turpis lectus malesuada tellus, a rutrum arcu velit a lorem. Quisque sagittis nunc eget dictum vulputate. Pellentesque congue accumsan dui non ullamcorper. Morbi aliquet, magna sit amet scelerisque vulputate, metus risus aliquam mauris, rhoncus tempus turpis sem quis ligula. Donec vitae nulla porta, lobortis eros non, egestas nisl. Sed non ipsum id neque auctor ultricies. Ut ultrices purus non ante suscipit dignissim."
			+ "\nMorbi aliquam dolor quis urna cursus laoreet. Pellentesque placerat in massa et bibendum. Sed augue mi, varius in vestibulum nec, placerat vel sapien. Nullam dictum sollicitudin ipsum, eu scelerisque dolor elementum non. Phasellus a mauris ut urna viverra consequat. Pellentesque feugiat venenatis diam, ut efficitur neque vestibulum sed. Donec eu tellus quam. Maecenas et purus maximus, consectetur eros sed, lobortis lectus. Cras vitae tortor neque. Vivamus at ipsum erat. Nam molestie felis est, quis dictum diam varius sit amet. Donec imperdiet quam at lorem aliquet, in hendrerit elit consequat."
			+ "\nPellentesque nec pretium magna. Cras eget suscipit urna. Morbi sed volutpat quam, sit amet volutpat ex. Nulla id mi luctus, vulputate lorem vel, tristique lacus. Donec a hendrerit ex. Morbi id ante diam. Proin tincidunt purus ac nibh mollis porta. In gravida condimentum gravida. In molestie porttitor.";
	private Buffers b;

	@BeforeEach
	public void init() throws InterruptedException {
		b = new Buffers();
		b.write(lorem.getBytes());
	}

	@Test
	public void indexOf() throws InterruptedException {
		assertEquals(lorem.indexOf('L'), BuffersUtils.indexOf(b, (byte) 'L', 0, -1));
		assertEquals(lorem.indexOf('.'), BuffersUtils.indexOf(b, (byte) '.', 0, -1));
		assertEquals(lorem.indexOf('.', 4096), BuffersUtils.indexOf(b, (byte) '.', 4096, -1));
	}

	@Test
	public void indexOfBloc() throws InterruptedException {
		assertEquals(lorem.indexOf("Lo"), BuffersUtils.indexOf(b, "Lo".getBytes(), 0, -1));
		assertEquals(lorem.indexOf(". "), BuffersUtils.indexOf(b, ". ".getBytes(), 0, -1));
		assertEquals(lorem.indexOf(". ", 4096), BuffersUtils.indexOf(b, ". ".getBytes(), 4096, -1));
	}

	@Test
	public void startsWith() throws InterruptedException {
		assertTrue(BuffersUtils.startsWith(b, "Lo".getBytes(), 0, -1));
		assertFalse(BuffersUtils.startsWith(b, "La".getBytes(), 0, -1));

		assertTrue(BuffersUtils.startsWith(b, ". ".getBytes(), lorem.indexOf(". "), -1));
		assertFalse(BuffersUtils.startsWith(b, "..".getBytes(), lorem.indexOf(". "), -1));

		assertTrue(BuffersUtils.startsWith(b, ". ".getBytes(), lorem.indexOf(". ", 4096), -1));
		assertFalse(BuffersUtils.startsWith(b, "..".getBytes(), lorem.indexOf(". ", 4096), -1));
	}

	@Test
	public void testToString() throws InterruptedException {
		StringBuilder sb = new StringBuilder();
		BuffersUtils.toString(sb, b, 0, -1);
		assertEquals(lorem, sb.toString());

		sb.setLength(0);
		BuffersUtils.toString(sb, b, 300, 200);
		assertEquals(lorem.substring(300, 500), sb.toString());

		sb.setLength(0);
		BuffersUtils.toString(sb, b, 4000, 200);
		assertEquals(lorem.substring(4000, 4200), sb.toString());
	}
}
