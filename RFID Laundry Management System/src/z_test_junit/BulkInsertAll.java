package z_test_junit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(value = Suite.class)
@SuiteClasses(value={
		BulkInsertStaffAndCloth.class,
		BulkInsertTransaction.class,
		})
public class BulkInsertAll {

}
