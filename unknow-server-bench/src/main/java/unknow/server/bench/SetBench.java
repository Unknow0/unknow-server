package unknow.server.bench;

import java.util.HashSet;
import java.util.Set;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

import unknow.server.util.data.ArraySet;

@BenchmarkMode(Mode.AverageTime)
public class SetBench {
	private static final Set<String> hashSet;
	private static final Set<String> arraySet;
	static {
		hashSet = new HashSet<>();
		for (int i = 0; i < 1000; i++)
			hashSet.add("a" + i);
		arraySet = new ArraySet<>(hashSet);
	}

	@Benchmark
	public void hashSet(final Blackhole bh) {
		bh.consume(hashSet.contains("a0"));
		bh.consume(hashSet.contains("a500"));
		bh.consume(hashSet.contains("a1000"));
		bh.consume(hashSet.contains("b1000"));
	}

	@Benchmark
	public void arraySet(final Blackhole bh) {
		bh.consume(arraySet.contains("a0"));
		bh.consume(arraySet.contains("a500"));
		bh.consume(arraySet.contains("a1000"));
		bh.consume(arraySet.contains("b1000"));
	}

	@Benchmark
	public void ifSet(final Blackhole bh) {
		bh.consume(contains("a0"));
		bh.consume(contains("a500"));
		bh.consume(contains("a1000"));
		bh.consume(contains("b1000"));
	}

	private boolean contains(String k) {
		return "a0".equals(k) || "a1".equals(k) || "a2".equals(k) || "a3".equals(k) || "a4".equals(k) || "a5".equals(k) || "a6".equals(k) || "a7".equals(k) || "a8".equals(k)
				|| "a9".equals(k) || "a10".equals(k) || "a11".equals(k) || "a12".equals(k) || "a13".equals(k) || "a14".equals(k) || "a15".equals(k) || "a16".equals(k)
				|| "a17".equals(k) || "a18".equals(k) || "a19".equals(k) || "a20".equals(k) || "a21".equals(k) || "a22".equals(k) || "a23".equals(k) || "a24".equals(k)
				|| "a25".equals(k) || "a26".equals(k) || "a27".equals(k) || "a28".equals(k) || "a29".equals(k) || "a30".equals(k) || "a31".equals(k) || "a32".equals(k)
				|| "a33".equals(k) || "a34".equals(k) || "a35".equals(k) || "a36".equals(k) || "a37".equals(k) || "a38".equals(k) || "a39".equals(k) || "a40".equals(k)
				|| "a41".equals(k) || "a42".equals(k) || "a43".equals(k) || "a44".equals(k) || "a45".equals(k) || "a46".equals(k) || "a47".equals(k) || "a48".equals(k)
				|| "a49".equals(k) || "a50".equals(k) || "a51".equals(k) || "a52".equals(k) || "a53".equals(k) || "a54".equals(k) || "a55".equals(k) || "a56".equals(k)
				|| "a57".equals(k) || "a58".equals(k) || "a59".equals(k) || "a60".equals(k) || "a61".equals(k) || "a62".equals(k) || "a63".equals(k) || "a64".equals(k)
				|| "a65".equals(k) || "a66".equals(k) || "a67".equals(k) || "a68".equals(k) || "a69".equals(k) || "a70".equals(k) || "a71".equals(k) || "a72".equals(k)
				|| "a73".equals(k) || "a74".equals(k) || "a75".equals(k) || "a76".equals(k) || "a77".equals(k) || "a78".equals(k) || "a79".equals(k) || "a80".equals(k)
				|| "a81".equals(k) || "a82".equals(k) || "a83".equals(k) || "a84".equals(k) || "a85".equals(k) || "a86".equals(k) || "a87".equals(k) || "a88".equals(k)
				|| "a89".equals(k) || "a90".equals(k) || "a91".equals(k) || "a92".equals(k) || "a93".equals(k) || "a94".equals(k) || "a95".equals(k) || "a96".equals(k)
				|| "a97".equals(k) || "a98".equals(k) || "a99".equals(k) || "a100".equals(k) || "a101".equals(k) || "a102".equals(k) || "a103".equals(k) || "a104".equals(k)
				|| "a105".equals(k) || "a106".equals(k) || "a107".equals(k) || "a108".equals(k) || "a109".equals(k) || "a110".equals(k) || "a111".equals(k) || "a112".equals(k)
				|| "a113".equals(k) || "a114".equals(k) || "a115".equals(k) || "a116".equals(k) || "a117".equals(k) || "a118".equals(k) || "a119".equals(k) || "a120".equals(k)
				|| "a121".equals(k) || "a122".equals(k) || "a123".equals(k) || "a124".equals(k) || "a125".equals(k) || "a126".equals(k) || "a127".equals(k) || "a128".equals(k)
				|| "a129".equals(k) || "a130".equals(k) || "a131".equals(k) || "a132".equals(k) || "a133".equals(k) || "a134".equals(k) || "a135".equals(k) || "a136".equals(k)
				|| "a137".equals(k) || "a138".equals(k) || "a139".equals(k) || "a140".equals(k) || "a141".equals(k) || "a142".equals(k) || "a143".equals(k) || "a144".equals(k)
				|| "a145".equals(k) || "a146".equals(k) || "a147".equals(k) || "a148".equals(k) || "a149".equals(k) || "a150".equals(k) || "a151".equals(k) || "a152".equals(k)
				|| "a153".equals(k) || "a154".equals(k) || "a155".equals(k) || "a156".equals(k) || "a157".equals(k) || "a158".equals(k) || "a159".equals(k) || "a160".equals(k)
				|| "a161".equals(k) || "a162".equals(k) || "a163".equals(k) || "a164".equals(k) || "a165".equals(k) || "a166".equals(k) || "a167".equals(k) || "a168".equals(k)
				|| "a169".equals(k) || "a170".equals(k) || "a171".equals(k) || "a172".equals(k) || "a173".equals(k) || "a174".equals(k) || "a175".equals(k) || "a176".equals(k)
				|| "a177".equals(k) || "a178".equals(k) || "a179".equals(k) || "a180".equals(k) || "a181".equals(k) || "a182".equals(k) || "a183".equals(k) || "a184".equals(k)
				|| "a185".equals(k) || "a186".equals(k) || "a187".equals(k) || "a188".equals(k) || "a189".equals(k) || "a190".equals(k) || "a191".equals(k) || "a192".equals(k)
				|| "a193".equals(k) || "a194".equals(k) || "a195".equals(k) || "a196".equals(k) || "a197".equals(k) || "a198".equals(k) || "a199".equals(k) || "a200".equals(k)
				|| "a201".equals(k) || "a202".equals(k) || "a203".equals(k) || "a204".equals(k) || "a205".equals(k) || "a206".equals(k) || "a207".equals(k) || "a208".equals(k)
				|| "a209".equals(k) || "a210".equals(k) || "a211".equals(k) || "a212".equals(k) || "a213".equals(k) || "a214".equals(k) || "a215".equals(k) || "a216".equals(k)
				|| "a217".equals(k) || "a218".equals(k) || "a219".equals(k) || "a220".equals(k) || "a221".equals(k) || "a222".equals(k) || "a223".equals(k) || "a224".equals(k)
				|| "a225".equals(k) || "a226".equals(k) || "a227".equals(k) || "a228".equals(k) || "a229".equals(k) || "a230".equals(k) || "a231".equals(k) || "a232".equals(k)
				|| "a233".equals(k) || "a234".equals(k) || "a235".equals(k) || "a236".equals(k) || "a237".equals(k) || "a238".equals(k) || "a239".equals(k) || "a240".equals(k)
				|| "a241".equals(k) || "a242".equals(k) || "a243".equals(k) || "a244".equals(k) || "a245".equals(k) || "a246".equals(k) || "a247".equals(k) || "a248".equals(k)
				|| "a249".equals(k) || "a250".equals(k) || "a251".equals(k) || "a252".equals(k) || "a253".equals(k) || "a254".equals(k) || "a255".equals(k) || "a256".equals(k)
				|| "a257".equals(k) || "a258".equals(k) || "a259".equals(k) || "a260".equals(k) || "a261".equals(k) || "a262".equals(k) || "a263".equals(k) || "a264".equals(k)
				|| "a265".equals(k) || "a266".equals(k) || "a267".equals(k) || "a268".equals(k) || "a269".equals(k) || "a270".equals(k) || "a271".equals(k) || "a272".equals(k)
				|| "a273".equals(k) || "a274".equals(k) || "a275".equals(k) || "a276".equals(k) || "a277".equals(k) || "a278".equals(k) || "a279".equals(k) || "a280".equals(k)
				|| "a281".equals(k) || "a282".equals(k) || "a283".equals(k) || "a284".equals(k) || "a285".equals(k) || "a286".equals(k) || "a287".equals(k) || "a288".equals(k)
				|| "a289".equals(k) || "a290".equals(k) || "a291".equals(k) || "a292".equals(k) || "a293".equals(k) || "a294".equals(k) || "a295".equals(k) || "a296".equals(k)
				|| "a297".equals(k) || "a298".equals(k) || "a299".equals(k) || "a300".equals(k) || "a301".equals(k) || "a302".equals(k) || "a303".equals(k) || "a304".equals(k)
				|| "a305".equals(k) || "a306".equals(k) || "a307".equals(k) || "a308".equals(k) || "a309".equals(k) || "a310".equals(k) || "a311".equals(k) || "a312".equals(k)
				|| "a313".equals(k) || "a314".equals(k) || "a315".equals(k) || "a316".equals(k) || "a317".equals(k) || "a318".equals(k) || "a319".equals(k) || "a320".equals(k)
				|| "a321".equals(k) || "a322".equals(k) || "a323".equals(k) || "a324".equals(k) || "a325".equals(k) || "a326".equals(k) || "a327".equals(k) || "a328".equals(k)
				|| "a329".equals(k) || "a330".equals(k) || "a331".equals(k) || "a332".equals(k) || "a333".equals(k) || "a334".equals(k) || "a335".equals(k) || "a336".equals(k)
				|| "a337".equals(k) || "a338".equals(k) || "a339".equals(k) || "a340".equals(k) || "a341".equals(k) || "a342".equals(k) || "a343".equals(k) || "a344".equals(k)
				|| "a345".equals(k) || "a346".equals(k) || "a347".equals(k) || "a348".equals(k) || "a349".equals(k) || "a350".equals(k) || "a351".equals(k) || "a352".equals(k)
				|| "a353".equals(k) || "a354".equals(k) || "a355".equals(k) || "a356".equals(k) || "a357".equals(k) || "a358".equals(k) || "a359".equals(k) || "a360".equals(k)
				|| "a361".equals(k) || "a362".equals(k) || "a363".equals(k) || "a364".equals(k) || "a365".equals(k) || "a366".equals(k) || "a367".equals(k) || "a368".equals(k)
				|| "a369".equals(k) || "a370".equals(k) || "a371".equals(k) || "a372".equals(k) || "a373".equals(k) || "a374".equals(k) || "a375".equals(k) || "a376".equals(k)
				|| "a377".equals(k) || "a378".equals(k) || "a379".equals(k) || "a380".equals(k) || "a381".equals(k) || "a382".equals(k) || "a383".equals(k) || "a384".equals(k)
				|| "a385".equals(k) || "a386".equals(k) || "a387".equals(k) || "a388".equals(k) || "a389".equals(k) || "a390".equals(k) || "a391".equals(k) || "a392".equals(k)
				|| "a393".equals(k) || "a394".equals(k) || "a395".equals(k) || "a396".equals(k) || "a397".equals(k) || "a398".equals(k) || "a399".equals(k) || "a400".equals(k)
				|| "a401".equals(k) || "a402".equals(k) || "a403".equals(k) || "a404".equals(k) || "a405".equals(k) || "a406".equals(k) || "a407".equals(k) || "a408".equals(k)
				|| "a409".equals(k) || "a410".equals(k) || "a411".equals(k) || "a412".equals(k) || "a413".equals(k) || "a414".equals(k) || "a415".equals(k) || "a416".equals(k)
				|| "a417".equals(k) || "a418".equals(k) || "a419".equals(k) || "a420".equals(k) || "a421".equals(k) || "a422".equals(k) || "a423".equals(k) || "a424".equals(k)
				|| "a425".equals(k) || "a426".equals(k) || "a427".equals(k) || "a428".equals(k) || "a429".equals(k) || "a430".equals(k) || "a431".equals(k) || "a432".equals(k)
				|| "a433".equals(k) || "a434".equals(k) || "a435".equals(k) || "a436".equals(k) || "a437".equals(k) || "a438".equals(k) || "a439".equals(k) || "a440".equals(k)
				|| "a441".equals(k) || "a442".equals(k) || "a443".equals(k) || "a444".equals(k) || "a445".equals(k) || "a446".equals(k) || "a447".equals(k) || "a448".equals(k)
				|| "a449".equals(k) || "a450".equals(k) || "a451".equals(k) || "a452".equals(k) || "a453".equals(k) || "a454".equals(k) || "a455".equals(k) || "a456".equals(k)
				|| "a457".equals(k) || "a458".equals(k) || "a459".equals(k) || "a460".equals(k) || "a461".equals(k) || "a462".equals(k) || "a463".equals(k) || "a464".equals(k)
				|| "a465".equals(k) || "a466".equals(k) || "a467".equals(k) || "a468".equals(k) || "a469".equals(k) || "a470".equals(k) || "a471".equals(k) || "a472".equals(k)
				|| "a473".equals(k) || "a474".equals(k) || "a475".equals(k) || "a476".equals(k) || "a477".equals(k) || "a478".equals(k) || "a479".equals(k) || "a480".equals(k)
				|| "a481".equals(k) || "a482".equals(k) || "a483".equals(k) || "a484".equals(k) || "a485".equals(k) || "a486".equals(k) || "a487".equals(k) || "a488".equals(k)
				|| "a489".equals(k) || "a490".equals(k) || "a491".equals(k) || "a492".equals(k) || "a493".equals(k) || "a494".equals(k) || "a495".equals(k) || "a496".equals(k)
				|| "a497".equals(k) || "a498".equals(k) || "a499".equals(k) || "a500".equals(k) || "a501".equals(k) || "a502".equals(k) || "a503".equals(k) || "a504".equals(k)
				|| "a505".equals(k) || "a506".equals(k) || "a507".equals(k) || "a508".equals(k) || "a509".equals(k) || "a510".equals(k) || "a511".equals(k) || "a512".equals(k)
				|| "a513".equals(k) || "a514".equals(k) || "a515".equals(k) || "a516".equals(k) || "a517".equals(k) || "a518".equals(k) || "a519".equals(k) || "a520".equals(k)
				|| "a521".equals(k) || "a522".equals(k) || "a523".equals(k) || "a524".equals(k) || "a525".equals(k) || "a526".equals(k) || "a527".equals(k) || "a528".equals(k)
				|| "a529".equals(k) || "a530".equals(k) || "a531".equals(k) || "a532".equals(k) || "a533".equals(k) || "a534".equals(k) || "a535".equals(k) || "a536".equals(k)
				|| "a537".equals(k) || "a538".equals(k) || "a539".equals(k) || "a540".equals(k) || "a541".equals(k) || "a542".equals(k) || "a543".equals(k) || "a544".equals(k)
				|| "a545".equals(k) || "a546".equals(k) || "a547".equals(k) || "a548".equals(k) || "a549".equals(k) || "a550".equals(k) || "a551".equals(k) || "a552".equals(k)
				|| "a553".equals(k) || "a554".equals(k) || "a555".equals(k) || "a556".equals(k) || "a557".equals(k) || "a558".equals(k) || "a559".equals(k) || "a560".equals(k)
				|| "a561".equals(k) || "a562".equals(k) || "a563".equals(k) || "a564".equals(k) || "a565".equals(k) || "a566".equals(k) || "a567".equals(k) || "a568".equals(k)
				|| "a569".equals(k) || "a570".equals(k) || "a571".equals(k) || "a572".equals(k) || "a573".equals(k) || "a574".equals(k) || "a575".equals(k) || "a576".equals(k)
				|| "a577".equals(k) || "a578".equals(k) || "a579".equals(k) || "a580".equals(k) || "a581".equals(k) || "a582".equals(k) || "a583".equals(k) || "a584".equals(k)
				|| "a585".equals(k) || "a586".equals(k) || "a587".equals(k) || "a588".equals(k) || "a589".equals(k) || "a590".equals(k) || "a591".equals(k) || "a592".equals(k)
				|| "a593".equals(k) || "a594".equals(k) || "a595".equals(k) || "a596".equals(k) || "a597".equals(k) || "a598".equals(k) || "a599".equals(k) || "a600".equals(k)
				|| "a601".equals(k) || "a602".equals(k) || "a603".equals(k) || "a604".equals(k) || "a605".equals(k) || "a606".equals(k) || "a607".equals(k) || "a608".equals(k)
				|| "a609".equals(k) || "a610".equals(k) || "a611".equals(k) || "a612".equals(k) || "a613".equals(k) || "a614".equals(k) || "a615".equals(k) || "a616".equals(k)
				|| "a617".equals(k) || "a618".equals(k) || "a619".equals(k) || "a620".equals(k) || "a621".equals(k) || "a622".equals(k) || "a623".equals(k) || "a624".equals(k)
				|| "a625".equals(k) || "a626".equals(k) || "a627".equals(k) || "a628".equals(k) || "a629".equals(k) || "a630".equals(k) || "a631".equals(k) || "a632".equals(k)
				|| "a633".equals(k) || "a634".equals(k) || "a635".equals(k) || "a636".equals(k) || "a637".equals(k) || "a638".equals(k) || "a639".equals(k) || "a640".equals(k)
				|| "a641".equals(k) || "a642".equals(k) || "a643".equals(k) || "a644".equals(k) || "a645".equals(k) || "a646".equals(k) || "a647".equals(k) || "a648".equals(k)
				|| "a649".equals(k) || "a650".equals(k) || "a651".equals(k) || "a652".equals(k) || "a653".equals(k) || "a654".equals(k) || "a655".equals(k) || "a656".equals(k)
				|| "a657".equals(k) || "a658".equals(k) || "a659".equals(k) || "a660".equals(k) || "a661".equals(k) || "a662".equals(k) || "a663".equals(k) || "a664".equals(k)
				|| "a665".equals(k) || "a666".equals(k) || "a667".equals(k) || "a668".equals(k) || "a669".equals(k) || "a670".equals(k) || "a671".equals(k) || "a672".equals(k)
				|| "a673".equals(k) || "a674".equals(k) || "a675".equals(k) || "a676".equals(k) || "a677".equals(k) || "a678".equals(k) || "a679".equals(k) || "a680".equals(k)
				|| "a681".equals(k) || "a682".equals(k) || "a683".equals(k) || "a684".equals(k) || "a685".equals(k) || "a686".equals(k) || "a687".equals(k) || "a688".equals(k)
				|| "a689".equals(k) || "a690".equals(k) || "a691".equals(k) || "a692".equals(k) || "a693".equals(k) || "a694".equals(k) || "a695".equals(k) || "a696".equals(k)
				|| "a697".equals(k) || "a698".equals(k) || "a699".equals(k) || "a700".equals(k) || "a701".equals(k) || "a702".equals(k) || "a703".equals(k) || "a704".equals(k)
				|| "a705".equals(k) || "a706".equals(k) || "a707".equals(k) || "a708".equals(k) || "a709".equals(k) || "a710".equals(k) || "a711".equals(k) || "a712".equals(k)
				|| "a713".equals(k) || "a714".equals(k) || "a715".equals(k) || "a716".equals(k) || "a717".equals(k) || "a718".equals(k) || "a719".equals(k) || "a720".equals(k)
				|| "a721".equals(k) || "a722".equals(k) || "a723".equals(k) || "a724".equals(k) || "a725".equals(k) || "a726".equals(k) || "a727".equals(k) || "a728".equals(k)
				|| "a729".equals(k) || "a730".equals(k) || "a731".equals(k) || "a732".equals(k) || "a733".equals(k) || "a734".equals(k) || "a735".equals(k) || "a736".equals(k)
				|| "a737".equals(k) || "a738".equals(k) || "a739".equals(k) || "a740".equals(k) || "a741".equals(k) || "a742".equals(k) || "a743".equals(k) || "a744".equals(k)
				|| "a745".equals(k) || "a746".equals(k) || "a747".equals(k) || "a748".equals(k) || "a749".equals(k) || "a750".equals(k) || "a751".equals(k) || "a752".equals(k)
				|| "a753".equals(k) || "a754".equals(k) || "a755".equals(k) || "a756".equals(k) || "a757".equals(k) || "a758".equals(k) || "a759".equals(k) || "a760".equals(k)
				|| "a761".equals(k) || "a762".equals(k) || "a763".equals(k) || "a764".equals(k) || "a765".equals(k) || "a766".equals(k) || "a767".equals(k) || "a768".equals(k)
				|| "a769".equals(k) || "a770".equals(k) || "a771".equals(k) || "a772".equals(k) || "a773".equals(k) || "a774".equals(k) || "a775".equals(k) || "a776".equals(k)
				|| "a777".equals(k) || "a778".equals(k) || "a779".equals(k) || "a780".equals(k) || "a781".equals(k) || "a782".equals(k) || "a783".equals(k) || "a784".equals(k)
				|| "a785".equals(k) || "a786".equals(k) || "a787".equals(k) || "a788".equals(k) || "a789".equals(k) || "a790".equals(k) || "a791".equals(k) || "a792".equals(k)
				|| "a793".equals(k) || "a794".equals(k) || "a795".equals(k) || "a796".equals(k) || "a797".equals(k) || "a798".equals(k) || "a799".equals(k) || "a800".equals(k)
				|| "a801".equals(k) || "a802".equals(k) || "a803".equals(k) || "a804".equals(k) || "a805".equals(k) || "a806".equals(k) || "a807".equals(k) || "a808".equals(k)
				|| "a809".equals(k) || "a810".equals(k) || "a811".equals(k) || "a812".equals(k) || "a813".equals(k) || "a814".equals(k) || "a815".equals(k) || "a816".equals(k)
				|| "a817".equals(k) || "a818".equals(k) || "a819".equals(k) || "a820".equals(k) || "a821".equals(k) || "a822".equals(k) || "a823".equals(k) || "a824".equals(k)
				|| "a825".equals(k) || "a826".equals(k) || "a827".equals(k) || "a828".equals(k) || "a829".equals(k) || "a830".equals(k) || "a831".equals(k) || "a832".equals(k)
				|| "a833".equals(k) || "a834".equals(k) || "a835".equals(k) || "a836".equals(k) || "a837".equals(k) || "a838".equals(k) || "a839".equals(k) || "a840".equals(k)
				|| "a841".equals(k) || "a842".equals(k) || "a843".equals(k) || "a844".equals(k) || "a845".equals(k) || "a846".equals(k) || "a847".equals(k) || "a848".equals(k)
				|| "a849".equals(k) || "a850".equals(k) || "a851".equals(k) || "a852".equals(k) || "a853".equals(k) || "a854".equals(k) || "a855".equals(k) || "a856".equals(k)
				|| "a857".equals(k) || "a858".equals(k) || "a859".equals(k) || "a860".equals(k) || "a861".equals(k) || "a862".equals(k) || "a863".equals(k) || "a864".equals(k)
				|| "a865".equals(k) || "a866".equals(k) || "a867".equals(k) || "a868".equals(k) || "a869".equals(k) || "a870".equals(k) || "a871".equals(k) || "a872".equals(k)
				|| "a873".equals(k) || "a874".equals(k) || "a875".equals(k) || "a876".equals(k) || "a877".equals(k) || "a878".equals(k) || "a879".equals(k) || "a880".equals(k)
				|| "a881".equals(k) || "a882".equals(k) || "a883".equals(k) || "a884".equals(k) || "a885".equals(k) || "a886".equals(k) || "a887".equals(k) || "a888".equals(k)
				|| "a889".equals(k) || "a890".equals(k) || "a891".equals(k) || "a892".equals(k) || "a893".equals(k) || "a894".equals(k) || "a895".equals(k) || "a896".equals(k)
				|| "a897".equals(k) || "a898".equals(k) || "a899".equals(k) || "a900".equals(k) || "a901".equals(k) || "a902".equals(k) || "a903".equals(k) || "a904".equals(k)
				|| "a905".equals(k) || "a906".equals(k) || "a907".equals(k) || "a908".equals(k) || "a909".equals(k) || "a910".equals(k) || "a911".equals(k) || "a912".equals(k)
				|| "a913".equals(k) || "a914".equals(k) || "a915".equals(k) || "a916".equals(k) || "a917".equals(k) || "a918".equals(k) || "a919".equals(k) || "a920".equals(k)
				|| "a921".equals(k) || "a922".equals(k) || "a923".equals(k) || "a924".equals(k) || "a925".equals(k) || "a926".equals(k) || "a927".equals(k) || "a928".equals(k)
				|| "a929".equals(k) || "a930".equals(k) || "a931".equals(k) || "a932".equals(k) || "a933".equals(k) || "a934".equals(k) || "a935".equals(k) || "a936".equals(k)
				|| "a937".equals(k) || "a938".equals(k) || "a939".equals(k) || "a940".equals(k) || "a941".equals(k) || "a942".equals(k) || "a943".equals(k) || "a944".equals(k)
				|| "a945".equals(k) || "a946".equals(k) || "a947".equals(k) || "a948".equals(k) || "a949".equals(k) || "a950".equals(k) || "a951".equals(k) || "a952".equals(k)
				|| "a953".equals(k) || "a954".equals(k) || "a955".equals(k) || "a956".equals(k) || "a957".equals(k) || "a958".equals(k) || "a959".equals(k) || "a960".equals(k)
				|| "a961".equals(k) || "a962".equals(k) || "a963".equals(k) || "a964".equals(k) || "a965".equals(k) || "a966".equals(k) || "a967".equals(k) || "a968".equals(k)
				|| "a969".equals(k) || "a970".equals(k) || "a971".equals(k) || "a972".equals(k) || "a973".equals(k) || "a974".equals(k) || "a975".equals(k) || "a976".equals(k)
				|| "a977".equals(k) || "a978".equals(k) || "a979".equals(k) || "a980".equals(k) || "a981".equals(k) || "a982".equals(k) || "a983".equals(k) || "a984".equals(k)
				|| "a985".equals(k) || "a986".equals(k) || "a987".equals(k) || "a988".equals(k) || "a989".equals(k) || "a990".equals(k) || "a991".equals(k) || "a992".equals(k)
				|| "a993".equals(k) || "a994".equals(k) || "a995".equals(k) || "a996".equals(k) || "a997".equals(k) || "a998".equals(k) || "a999".equals(k);
	}
}
