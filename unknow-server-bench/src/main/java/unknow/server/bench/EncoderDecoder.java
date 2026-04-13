package unknow.server.bench;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import unknow.server.util.Decoder;
import unknow.server.util.Encoder;

public class EncoderDecoder {
	private static final String LATIN = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet."
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit sodales. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede pellentesque fermentum. Maecenas adipiscing ante non diam sodales hendrerit.\r\n"
			+ "\r\n"
			+ "Ut velit mauris, egestas sed, gravida nec, ornare ut, mi. Aenean ut orci vel massa suscipit pulvinar. Nulla sollicitudin. Fusce varius, ligula non tempus aliquam, nunc turpis ullamcorper nibh, in tempus sapien eros vitae ligula. Pellentesque rhoncus nunc et augue. Integer id felis. Curabitur aliquet pellentesque diam. Integer quis metus vitae elit lobortis egestas. Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Morbi vel erat non mauris convallis vehicula. Nulla et sapien. Integer tortor tellus, aliquam faucibus, convallis id, congue eu, quam. Mauris ullamcorper felis vitae erat. Proin feugiat, augue non elementum posuere, metus purus iaculis lectus, et tristique ligula justo vitae magna.\r\n"
			+ "Aliquam convallis sollicitudin purus. Praesent aliquam, enim at fermentum mollis, ligula massa adipiscing nisl, ac euismod nibh nisl eu lectus. Fusce vulputate sem at sapien. Vivamus leo. Aliquam euismod libero eu enim. Nulla nec felis sed leo placerat imperdiet. Aenean suscipit nulla in justo. Suspendisse cursus rutrum augue. Nulla tincidunt tincidunt mi. Curabitur iaculis, lorem vel rhoncus faucibus, felis magna fermentum augue, et ultricies lacus lorem varius purus. Curabitur eu amet.";

	private static final String SIMPLE = "Hello world!\nÇa va bien ?\nПривет, как дела?\n你好，世界\nこんにちは世界\n👋🌍✨🔥🚀\nLorem ipsum dolor sit amet, consectetur adipiscing elit.";

	private static final String COMPLEX = "𓂀𓆣𓇼𓁹𓃰 𐍈𐌰𐌽𐌳𐌴𐍂 🌌✨🌀💫 🌍🌎🌏 🧬⚛️🔮 ∑∏√∞ ≈≠≡ ⌘⌬⌭ ⟁\r\n" + "如来如去 如夢如幻 🌸🌺🌼 美しい世界 🌈🌠 🌕🌖🌗🌘\r\n"
			+ "العربية العربية العربية ✨🌙⭐ ☯️☪️✡️ 🕉️ 🔱\r\n" + "\r\n" + "🐉🦄🐲🐾 🐉🦄🐲 \uD83E\uDDFF\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8\uFE0F 👁️👁️ 🌀🌀🌀\r\n"
			+ "👨‍🚀👩‍🚀🧑‍🚀 🚀🛸🚀🛸 🌌🌌🌌 💫💫💫\r\n" + "\r\n" + "𒀱𒂗𒆠 𒀭𒈹𒂊𒉡 𒌷𒆠𒂗𒄀 🏺🏺🏺\r\n" + "ᚠᛇᚻᚢᚦᚨᚱᚲ ⚔️🛡️ ⚔️🛡️\r\n" + "𑀓𑀺𑀢𑁆𑀢𑀺 𑀧𑀭𑀫𑀸𑀦 🌿🌿🌿\r\n"
			+ "\r\n" + "🌲🍃🌲🍃🌲 🍄🍄🍄 🌸🌸🌸\r\n" + "🌊🌊🌊 🌊🌊🌊 🐚🐚🐚 🐠🐟🐡\r\n" + "\r\n" + "💻📡📶 🧠🤖🧠 🤖🧠🤖 ⚙️⚙️⚙️\r\n" + "0101 ✨ 𐍈 0101 ✨ 𐍈\r\n" + "\r\n"
			+ "ქართული ქართული ქართული 🌄🌄\r\n" + "संस्कृतम् संस्कृतम् संस्कृतम् 🔱🔱\r\n" + "\r\n" + "🔥🔥🔥 ❄️❄️❄️ ⚡⚡⚡ 🌪️🌪️🌪️\r\n" + "❤️🧡💛💚💙💜🖤 🤍🤎 💖💗💓\r\n"
			+ "\r\n" + "🌐🌐🌐 🌐🌐🌐 🌀🌀🌀 🌀🌀🌀\r\n" + "💫✨🌟⭐✨💫 🌟✨⭐💫✨🌟\r\n" + "\r\n" + "𓂀𓂀𓂀 𓇼𓇼𓇼 𓆣𓆣𓆣\r\n" + "𐍈𐍈𐍈 ᚠᚠᚠ 𒀱𒀱𒀱"
			+ "𓂀𓆣𓇼𓁹𓃰 𐍈𐌰𐌽𐌳𐌴𐍂 🌌✨🌀💫 🌍🌎🌏 🧬⚛️🔮 ∑∏√∞ ≈≠≡ ⌘⌬⌭ ⟁\r\n" + "如来如去 如夢如幻 🌸🌺🌼 美しい世界 🌈🌠 🌕🌖🌗🌘\r\n"
			+ "العربية العربية العربية ✨🌙⭐ ☯️☪️✡️ 🕉️ 🔱\r\n" + "\r\n" + "🐉🦄🐲🐾 🐉🦄🐲 \uD83E\uDDFF\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8\uFE0F 👁️👁️ 🌀🌀🌀\r\n"
			+ "👨‍🚀👩‍🚀🧑‍🚀 🚀🛸🚀🛸 🌌🌌🌌 💫💫💫\r\n" + "\r\n" + "𒀱𒂗𒆠 𒀭𒈹𒂊𒉡 𒌷𒆠𒂗𒄀 🏺🏺🏺\r\n" + "ᚠᛇᚻᚢᚦᚨᚱᚲ ⚔️🛡️ ⚔️🛡️\r\n" + "𑀓𑀺𑀢𑁆𑀢𑀺 𑀧𑀭𑀫𑀸𑀦 🌿🌿🌿\r\n"
			+ "\r\n" + "🌲🍃🌲🍃🌲 🍄🍄🍄 🌸🌸🌸\r\n" + "🌊🌊🌊 🌊🌊🌊 🐚🐚🐚 🐠🐟🐡\r\n" + "\r\n" + "💻📡📶 🧠🤖🧠 🤖🧠🤖 ⚙️⚙️⚙️\r\n" + "0101 ✨ 𐍈 0101 ✨ 𐍈\r\n" + "\r\n"
			+ "ქართული ქართული ქართული 🌄🌄\r\n" + "संस्कृतम् संस्कृतम् संस्कृतम् 🔱🔱\r\n" + "\r\n" + "🔥🔥🔥 ❄️❄️❄️ ⚡⚡⚡ 🌪️🌪️🌪️\r\n" + "❤️🧡💛💚💙💜🖤 🤍🤎 💖💗💓\r\n"
			+ "\r\n" + "🌐🌐🌐 🌐🌐🌐 🌀🌀🌀 🌀🌀🌀\r\n" + "💫✨🌟⭐✨💫 🌟✨⭐💫✨🌟\r\n" + "\r\n" + "𓂀𓂀𓂀 𓇼𓇼𓇼 𓆣𓆣𓆣\r\n" + "𐍈𐍈𐍈 ᚠᚠᚠ 𒀱𒀱𒀱"
			+ "𓂀𓆣𓇼𓁹𓃰 𐍈𐌰𐌽𐌳𐌴𐍂 🌌✨🌀💫 🌍🌎🌏 🧬⚛️🔮 ∑∏√∞ ≈≠≡ ⌘⌬⌭ ⟁\r\n" + "如来如去 如夢如幻 🌸🌺🌼 美しい世界 🌈🌠 🌕🌖🌗🌘\r\n"
			+ "العربية العربية العربية ✨🌙⭐ ☯️☪️✡️ 🕉️ 🔱\r\n" + "\r\n" + "🐉🦄🐲🐾 🐉🦄🐲 \uD83E\uDDFF\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8\uFE0F 👁️👁️ 🌀🌀🌀\r\n"
			+ "👨‍🚀👩‍🚀🧑‍🚀 🚀🛸🚀🛸 🌌🌌🌌 💫💫💫\r\n" + "\r\n" + "𒀱𒂗𒆠 𒀭𒈹𒂊𒉡 𒌷𒆠𒂗𒄀 🏺🏺🏺\r\n" + "ᚠᛇᚻᚢᚦᚨᚱᚲ ⚔️🛡️ ⚔️🛡️\r\n" + "𑀓𑀺𑀢𑁆𑀢𑀺 𑀧𑀭𑀫𑀸𑀦 🌿🌿🌿\r\n"
			+ "\r\n" + "🌲🍃🌲🍃🌲 🍄🍄🍄 🌸🌸🌸\r\n" + "🌊🌊🌊 🌊🌊🌊 🐚🐚🐚 🐠🐟🐡\r\n" + "\r\n" + "💻📡📶 🧠🤖🧠 🤖🧠🤖 ⚙️⚙️⚙️\r\n" + "0101 ✨ 𐍈 0101 ✨ 𐍈\r\n" + "\r\n"
			+ "ქართული ქართული ქართული 🌄🌄\r\n" + "संस्कृतम् संस्कृतम् संस्कृतम् 🔱🔱\r\n" + "\r\n" + "🔥🔥🔥 ❄️❄️❄️ ⚡⚡⚡ 🌪️🌪️🌪️\r\n" + "❤️🧡💛💚💙💜🖤 🤍🤎 💖💗💓\r\n"
			+ "\r\n" + "🌐🌐🌐 🌐🌐🌐 🌀🌀🌀 🌀🌀🌀\r\n" + "💫✨🌟⭐✨💫 🌟✨⭐💫✨🌟\r\n" + "\r\n" + "𓂀𓂀𓂀 𓇼𓇼𓇼 𓆣𓆣𓆣\r\n" + "𐍈𐍈𐍈 ᚠᚠᚠ 𒀱𒀱𒀱"
			+ "𓂀𓆣𓇼𓁹𓃰 𐍈𐌰𐌽𐌳𐌴𐍂 🌌✨🌀💫 🌍🌎🌏 🧬⚛️🔮 ∑∏√∞ ≈≠≡ ⌘⌬⌭ ⟁\r\n" + "如来如去 如夢如幻 🌸🌺🌼 美しい世界 🌈🌠 🌕🌖🌗🌘\r\n"
			+ "العربية العربية العربية ✨🌙⭐ ☯️☪️✡️ 🕉️ 🔱\r\n" + "\r\n" + "🐉🦄🐲🐾 🐉🦄🐲 \uD83E\uDDFF\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8\uFE0F 👁️👁️ 🌀🌀🌀\r\n"
			+ "👨‍🚀👩‍🚀🧑‍🚀 🚀🛸🚀🛸 🌌🌌🌌 💫💫💫\r\n" + "\r\n" + "𒀱𒂗𒆠 𒀭𒈹𒂊𒉡 𒌷𒆠𒂗𒄀 🏺🏺🏺\r\n" + "ᚠᛇᚻᚢᚦᚨᚱᚲ ⚔️🛡️ ⚔️🛡️\r\n" + "𑀓𑀺𑀢𑁆𑀢𑀺 𑀧𑀭𑀫𑀸𑀦 🌿🌿🌿\r\n"
			+ "\r\n" + "🌲🍃🌲🍃🌲 🍄🍄🍄 🌸🌸🌸\r\n" + "🌊🌊🌊 🌊🌊🌊 🐚🐚🐚 🐠🐟🐡\r\n" + "\r\n" + "💻📡📶 🧠🤖🧠 🤖🧠🤖 ⚙️⚙️⚙️\r\n" + "0101 ✨ 𐍈 0101 ✨ 𐍈\r\n" + "\r\n"
			+ "ქართული ქართული ქართული 🌄🌄\r\n" + "संस्कृतम् संस्कृतम् संस्कृतम् 🔱🔱\r\n" + "\r\n" + "🔥🔥🔥 ❄️❄️❄️ ⚡⚡⚡ 🌪️🌪️🌪️\r\n" + "❤️🧡💛💚💙💜🖤 🤍🤎 💖💗💓\r\n"
			+ "\r\n" + "🌐🌐🌐 🌐🌐🌐 🌀🌀🌀 🌀🌀🌀\r\n" + "💫✨🌟⭐✨💫 🌟✨⭐💫✨🌟\r\n" + "\r\n" + "𓂀𓂀𓂀 𓇼𓇼𓇼 𓆣𓆣𓆣\r\n" + "𐍈𐍈𐍈 ᚠᚠᚠ 𒀱𒀱𒀱"
			+ "𓂀𓆣𓇼𓁹𓃰 𐍈𐌰𐌽𐌳𐌴𐍂 🌌✨🌀💫 🌍🌎🌏 🧬⚛️🔮 ∑∏√∞ ≈≠≡ ⌘⌬⌭ ⟁\r\n" + "如来如去 如夢如幻 🌸🌺🌼 美しい世界 🌈🌠 🌕🌖🌗🌘\r\n"
			+ "العربية العربية العربية ✨🌙⭐ ☯️☪️✡️ 🕉️ 🔱\r\n" + "\r\n" + "🐉🦄🐲🐾 🐉🦄🐲 \uD83E\uDDFF\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8\uFE0F 👁️👁️ 🌀🌀🌀\r\n"
			+ "👨‍🚀👩‍🚀🧑‍🚀 🚀🛸🚀🛸 🌌🌌🌌 💫💫💫\r\n" + "\r\n" + "𒀱𒂗𒆠 𒀭𒈹𒂊𒉡 𒌷𒆠𒂗𒄀 🏺🏺🏺\r\n" + "ᚠᛇᚻᚢᚦᚨᚱᚲ ⚔️🛡️ ⚔️🛡️\r\n" + "𑀓𑀺𑀢𑁆𑀢𑀺 𑀧𑀭𑀫𑀸𑀦 🌿🌿🌿\r\n"
			+ "\r\n" + "🌲🍃🌲🍃🌲 🍄🍄🍄 🌸🌸🌸\r\n" + "🌊🌊🌊 🌊🌊🌊 🐚🐚🐚 🐠🐟🐡\r\n" + "\r\n" + "💻📡📶 🧠🤖🧠 🤖🧠🤖 ⚙️⚙️⚙️\r\n" + "0101 ✨ 𐍈 0101 ✨ 𐍈\r\n" + "\r\n"
			+ "ქართული ქართული ქართული 🌄🌄\r\n" + "संस्कृतम् संस्कृतम् संस्कृतम् 🔱🔱\r\n" + "\r\n" + "🔥🔥🔥 ❄️❄️❄️ ⚡⚡⚡ 🌪️🌪️🌪️\r\n" + "❤️🧡💛💚💙💜🖤 🤍🤎 💖💗💓\r\n"
			+ "\r\n" + "🌐🌐🌐 🌐🌐🌐 🌀🌀🌀 🌀🌀🌀\r\n" + "💫✨🌟⭐✨💫 🌟✨⭐💫✨🌟\r\n" + "\r\n" + "𓂀𓂀𓂀 𓇼𓇼𓇼 𓆣𓆣𓆣\r\n" + "𐍈𐍈𐍈 ᚠᚠᚠ 𒀱𒀱𒀱"
			+ "𓂀𓆣𓇼𓁹𓃰 𐍈𐌰𐌽𐌳𐌴𐍂 🌌✨🌀💫 🌍🌎🌏 🧬⚛️🔮 ∑∏√∞ ≈≠≡ ⌘⌬⌭ ⟁\r\n" + "如来如去 如夢如幻 🌸🌺🌼 美しい世界 🌈🌠 🌕🌖🌗🌘\r\n"
			+ "العربية العربية العربية ✨🌙⭐ ☯️☪️✡️ 🕉️ 🔱\r\n" + "\r\n" + "🐉🦄🐲🐾 🐉🦄🐲 \uD83E\uDDFF\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8\uFE0F 👁️👁️ 🌀🌀🌀\r\n"
			+ "👨‍🚀👩‍🚀🧑‍🚀 🚀🛸🚀🛸 🌌🌌🌌 💫💫💫\r\n" + "\r\n" + "𒀱𒂗𒆠 𒀭𒈹𒂊𒉡 𒌷𒆠𒂗𒄀 🏺🏺🏺\r\n" + "ᚠᛇᚻᚢᚦᚨᚱᚲ ⚔️🛡️ ⚔️🛡️\r\n" + "𑀓𑀺𑀢𑁆𑀢𑀺 𑀧𑀭𑀫𑀸𑀦 🌿🌿🌿\r\n"
			+ "\r\n" + "🌲🍃🌲🍃🌲 🍄🍄🍄 🌸🌸🌸\r\n" + "🌊🌊🌊 🌊🌊🌊 🐚🐚🐚 🐠🐟🐡\r\n" + "\r\n" + "💻📡📶 🧠🤖🧠 🤖🧠🤖 ⚙️⚙️⚙️\r\n" + "0101 ✨ 𐍈 0101 ✨ 𐍈\r\n" + "\r\n"
			+ "ქართული ქართული ქართული 🌄🌄\r\n" + "संस्कृतम् संस्कृतम् संस्कृतम् 🔱🔱\r\n" + "\r\n" + "🔥🔥🔥 ❄️❄️❄️ ⚡⚡⚡ 🌪️🌪️🌪️\r\n" + "❤️🧡💛💚💙💜🖤 🤍🤎 💖💗💓\r\n"
			+ "\r\n" + "🌐🌐🌐 🌐🌐🌐 🌀🌀🌀 🌀🌀🌀\r\n" + "💫✨🌟⭐✨💫 🌟✨⭐💫✨🌟\r\n" + "\r\n" + "𓂀𓂀𓂀 𓇼𓇼𓇼 𓆣𓆣𓆣\r\n" + "𐍈𐍈𐍈 ᚠᚠᚠ 𒀱𒀱𒀱";

	@State(Scope.Thread)
	public static class Data {
		@Param({ "latin", "simple", "complex" })
		String name;
		CharBuffer cbuf;
		ByteBuffer bytes;

		@Setup
		public void setup() {
			switch (name) {
				case "latin":
					init(LATIN);
					break;
				case "simple":
					init(SIMPLE);
					break;
				case "complex":
					init(COMPLEX);
					break;
				default:
			}
		}

		private void init(String str) {
			cbuf = CharBuffer.allocate(str.length());
			str.getChars(0, str.length(), cbuf.array(), 0);
			bytes = ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8));
		}
	}

	@Benchmark
	public ByteBuffer encoderUtf8(Data data) {
		ByteBuffer b = ByteBuffer.allocate(4096);
		Encoder e = Encoder.from(StandardCharsets.UTF_8);
		CharBuffer cbuf = data.cbuf.duplicate();
		while (cbuf.hasRemaining()) {
			e.encode(cbuf, b, false);
			b.clear();
		}
		return b.clear();
	}

	@Benchmark
	public CharBuffer decoderUtf8(Data data) {
		CharBuffer c = CharBuffer.allocate(4096);
		Decoder d = Decoder.from(StandardCharsets.UTF_8);
		ByteBuffer bbuf = data.bytes.duplicate();
		while (bbuf.hasRemaining()) {
			d.decode(bbuf, c, false);
			c.clear();
		}
		return c.clear();
	}

	@Benchmark
	public ByteBuffer encoderCharset(Data data) {
		ByteBuffer b = ByteBuffer.allocate(4096);
		CharsetEncoder e = StandardCharsets.UTF_8.newEncoder();
		CharBuffer cbuf = data.cbuf.duplicate();
		while (cbuf.hasRemaining()) {
			e.encode(cbuf, b, false);
			b.clear();
		}
		return b.clear();
	}

	@Benchmark
	public CharBuffer decoderCharset(Data data) {
		CharBuffer c = CharBuffer.allocate(4096);
		CharsetDecoder d = StandardCharsets.UTF_8.newDecoder();
		ByteBuffer bbuf = data.bytes.duplicate();
		while (bbuf.hasRemaining()) {
			d.decode(bbuf, c, false);
			c.clear();
		}
		return c.clear();
	}

	@Benchmark
	public ByteBuffer slowEncoderUtf8(Data data) {
		ByteBuffer b = ByteBuffer.allocate(4096);
		Encoder e = Encoder.from(StandardCharsets.UTF_8);
		CharBuffer cbuf = data.cbuf.asReadOnlyBuffer();
		while (cbuf.hasRemaining()) {
			e.encode(cbuf, b, false);
			b.clear();
		}
		return b.clear();
	}

	@Benchmark
	public CharBuffer slowDecoderUtf8(Data data) {
		CharBuffer c = CharBuffer.allocate(4096);
		Decoder d = Decoder.from(StandardCharsets.UTF_8);
		ByteBuffer bbuf = data.bytes.asReadOnlyBuffer();
		while (bbuf.hasRemaining()) {
			d.decode(bbuf, c, false);
			c.clear();
		}
		return c.clear();
	}

	@Benchmark
	public ByteBuffer slowEncoderCharset(Data data) {
		ByteBuffer b = ByteBuffer.allocate(4096);
		CharsetEncoder e = StandardCharsets.UTF_8.newEncoder();
		CharBuffer cbuf = data.cbuf.asReadOnlyBuffer();
		while (cbuf.hasRemaining()) {
			e.encode(cbuf, b, false);
			b.clear();
		}
		return b.clear();
	}

	@Benchmark
	public CharBuffer slowDecoderCharset(Data data) {
		CharBuffer c = CharBuffer.allocate(4096);
		CharsetDecoder d = StandardCharsets.UTF_8.newDecoder();
		ByteBuffer bbuf = data.bytes.asReadOnlyBuffer();
		while (bbuf.hasRemaining()) {
			d.decode(bbuf, c, false);
			c.clear();
		}
		return c.clear();
	}
}
