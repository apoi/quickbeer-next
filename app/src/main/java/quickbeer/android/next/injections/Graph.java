package quickbeer.android.next.injections;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Component;
import quickbeer.android.next.QuickBeer;
import quickbeer.android.next.activities.ActivityBase;
import quickbeer.android.next.data.DataStoreModule;
import quickbeer.android.next.data.store.BeerSearchStore;
import quickbeer.android.next.data.store.BeerStore;
import quickbeer.android.next.data.store.NetworkRequestStatusStore;
import quickbeer.android.next.data.store.ReviewStore;
import quickbeer.android.next.fragments.BeerDetailsFragment;
import quickbeer.android.next.fragments.BeerListFragment;
import quickbeer.android.next.fragments.BeerSearchFragment;
import quickbeer.android.next.fragments.MainFragment;
import quickbeer.android.next.network.NetworkService;
import quickbeer.android.next.viewmodels.ViewModelModule;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        DataStoreModule.class,
        ViewModelModule.class,
        InstrumentationModule.class
})
public interface Graph {

    void inject(QuickBeer quickBeer);
    void inject(NetworkService networkService);

    void inject(ActivityBase activityBase);

    void inject(MainFragment mainFragment);
    void inject(BeerListFragment beerListFragment);
    void inject(BeerSearchFragment beerSearchFragment);
    void inject(BeerDetailsFragment beerDetailsFragment);

    void inject(BeerStore store);
    void inject(BeerSearchStore store);
    void inject(ReviewStore store);
    void inject(NetworkRequestStatusStore store);

    final class Initializer {
        public static Graph init(Application application) {
            return DaggerGraph.builder()
                    .applicationModule(new ApplicationModule(application))
                    .build();
        }
    }
}
