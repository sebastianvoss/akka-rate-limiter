import com.sebastianvoss.CircularArray
import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by sebastian on 19.04.16.
 */
class CircularArraySpec extends FlatSpec with Matchers {

  "A CircularArray of size 3" should "contain elements 4, 2, 3 after inserting elements 1, 2, 3, 4" in {
    val b = CircularArray[Int](3)
    (1 to 4).foreach(b.add)
    b.buffer should be(Array(4, 2, 3))
  }

  "A CircularArray of size 3" should "contain elements 1, 2, 3 after inserting elements 1, 2, 3" in {
    val b = CircularArray[Int](3)
    (1 to 3).foreach(b.add)
    b.buffer should be(Array(1, 2, 3))
  }

}
