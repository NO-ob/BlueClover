package org.floens.chan.core.repository;

import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;

import org.floens.chan.core.database.DatabaseManager;
import org.floens.chan.core.model.json.site.SiteConfig;
import org.floens.chan.core.model.orm.Filter;
import org.floens.chan.core.model.orm.Loadable;
import org.floens.chan.core.model.orm.SiteModel;
import org.floens.chan.core.settings.json.JsonSettings;
import org.floens.chan.core.site.Site;
import org.floens.chan.core.site.SiteIcon;
import org.floens.chan.core.site.SiteRegistry;
import org.floens.chan.core.site.sites.chan4.Chan4;
import org.floens.chan.core.site.sites.lainchan.Lainchan;
import org.floens.chan.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.HttpUrl;

@Singleton
public class SiteRepository {
    private static final String TAG = "SiteRepository";

    private DatabaseManager databaseManager;

    private Sites sitesObservable = new Sites();

    // Shortcut
    private static SiteRepository instance;

    public static Site forId(int id) {
        return instance.sitesObservable.forId(id);
    }

    @Inject
    public SiteRepository(DatabaseManager databaseManager) {
        instance = this;
        this.databaseManager = databaseManager;
    }

    public Sites all() {
        return sitesObservable;
    }

    public SiteModel byId(int id) {
        return databaseManager.runTask(databaseManager.getDatabaseSiteManager()
                .byId(id));
    }

    public void setId(SiteModel siteModel, int id) {
        databaseManager.runTask(databaseManager.getDatabaseSiteManager()
                .updateId(siteModel, id));
    }

    public void updateSiteUserSettingsAsync(SiteModel siteModel, JsonSettings jsonSettings) {
        siteModel.storeUserSettings(jsonSettings);
        databaseManager.runTaskAsync(databaseManager.getDatabaseSiteManager()
                .update(siteModel));
    }

    public Map<Integer, Integer> getOrdering() {
        return databaseManager.runTask(databaseManager.getDatabaseSiteManager().getOrdering());
    }

    public void updateSiteOrderingAsync(List<Site> sites) {
        List<Integer> ids = new ArrayList<>(sites.size());
        for (Site site : sites) {
            ids.add(site.id());
        }

        databaseManager.runTaskAsync(
                databaseManager.getDatabaseSiteManager().updateOrdering(ids),
                (r) -> {
                    sitesObservable.wasReordered();
                    sitesObservable.notifyObservers();
                });
    }

    public void initialize() {
        List<Site> sites = new ArrayList<>();

        List<SiteModel> models = databaseManager.runTask(
                databaseManager.getDatabaseSiteManager().getAll());

        for (SiteModel siteModel : models) {
            SiteConfigSettingsHolder holder;
            try {
                holder = instantiateSiteFromModel(siteModel);
            } catch (IllegalArgumentException e) {
                Logger.e(TAG, "instantiateSiteFromModel", e);
                break;
            }

            Site site = holder.site;
            SiteConfig config = holder.config;
            JsonSettings settings = holder.settings;

            site.initialize(siteModel.id, config, settings);

            sites.add(site);
        }

        sitesObservable.addAll(sites);

        for (Site site : sites) {
            site.postInitialize();
        }

        sitesObservable.notifyObservers();
    }

    // Called before #initialize to add the old 4chan site when the database was upgraded from
    // an older version. It only adds the model to the database with id 0.
    public void addLegacySite() {
        Site site = new Chan4();

        SiteConfig config = new SiteConfig();
        config.classId = SiteRegistry.SITE_CLASSES.keyAt(SiteRegistry.SITE_CLASSES.indexOfValue(site.getClass()));
        config.external = false;

        SiteModel model = createFromClass(config, new JsonSettings());
        setId(model, 0);
    }

    public Site createFromClass(Class<? extends Site> siteClass) {
        Site site = instantiateSiteClass(siteClass);

        SiteConfig config = new SiteConfig();
        JsonSettings settings = new JsonSettings();

        config.classId = SiteRegistry.SITE_CLASSES.keyAt(SiteRegistry.SITE_CLASSES.indexOfValue(site.getClass()));
        config.external = false;

        SiteModel model = createFromClass(config, settings);

        site.initialize(model.id, config, settings);

        sitesObservable.add(site);

        site.postInitialize();

        sitesObservable.notifyObservers();

        return site;
    }

    private SiteModel createFromClass(SiteConfig config, JsonSettings userSettings) {
        SiteModel siteModel = new SiteModel();
        siteModel.storeConfig(config);
        siteModel.storeUserSettings(userSettings);
        databaseManager.runTask(databaseManager.getDatabaseSiteManager().add(siteModel));

        return siteModel;
    }

    private SiteConfigSettingsHolder instantiateSiteFromModel(SiteModel siteModel) {
        Pair<SiteConfig, JsonSettings> configFields = siteModel.loadConfigFields();
        SiteConfig config = configFields.first;
        JsonSettings settings = configFields.second;

        return new SiteConfigSettingsHolder(
                instantiateSiteClass(config.classId),
                config,
                settings);
    }

    private Site instantiateSiteClass(int classId) {
        Class<? extends Site> clazz = SiteRegistry.SITE_CLASSES.get(classId);
        if (clazz == null) {
            //throw new IllegalArgumentException("Unknown class id");
            return instantiateSiteClass(InvalidSite.class);
        }
        return instantiateSiteClass(clazz);
    }

    private Site instantiateSiteClass(Class<? extends Site> clazz) {
        Site site;
        try {
            site = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException();
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException();
        }
        return site;
    }

    public void removeSite(Site site) {
        databaseManager.runTask(() -> {
            removeFilters(site);
            removeBoards(site);

            List<Loadable> siteLoadables = databaseManager.getDatabaseLoadableManager().getLoadables(site).call();
            if (!siteLoadables.isEmpty()) {
                removePins(siteLoadables);
                removeHistory(siteLoadables);
                removeLoadables(siteLoadables);
            }

            removeSavedReplies(site);
            removeThreadHides(site);

            databaseManager.getDatabaseSiteManager().deleteSite(site).call();
            return null;
        });
    }

    private void removeThreadHides(Site site) throws Exception {
        databaseManager.getDatabaseHideManager().deleteThreadHides(site).call();
    }

    private void removeSavedReplies(Site site) throws Exception {
        databaseManager.getDatabaseSavedReplyManager().deleteSavedReplies(site).call();
    }

    private void removeLoadables(List<Loadable> siteLoadables) throws Exception {
        databaseManager.getDatabaseLoadableManager().deleteLoadables(siteLoadables).call();
    }

    private void removeHistory(List<Loadable> siteLoadables) throws Exception {
        databaseManager.getDatabaseHistoryManager().deleteHistory(siteLoadables).call();
    }

    private void removePins(List<Loadable> siteLoadables) throws Exception {
        databaseManager.getDatabasePinManager().deletePins(siteLoadables).call();
    }

    private void removeBoards(Site site) throws Exception {
        databaseManager.getDatabaseBoardManager().deleteBoards(site).call();
    }

    private void removeFilters(Site site) throws Exception {
        List<Filter> filtersToDelete = new ArrayList<>();

        for (Filter filter : databaseManager.getDatabaseFilterManager().getFilters().call()) {
            if (filter.allBoards) {
                continue;
            }

            if (TextUtils.isEmpty(filter.boards)) {
                continue;
            }

            for (String uniqueId : filter.boards.split(",")) {
                String[] split = uniqueId.split(":");
                if (split.length != 2) {
                    continue;
                }

                try {
                    if (Integer.parseInt(split[0]) == site.id()) {
                        filtersToDelete.add(filter);
                        break;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        databaseManager.getDatabaseFilterManager().deleteFilters(filtersToDelete).call();
    }

    public class Sites extends Observable {
        private List<Site> sites = Collections.unmodifiableList(new ArrayList<>());
        private SparseArray<Site> sitesById = new SparseArray<>();

        public Site forId(int id) {
            Site s = sitesById.get(id);
            if (s == null) {
                throw new IllegalArgumentException("No site with id (" + id + ")");
            }
            return s;
        }

        public List<Site> getAll() {
            return new ArrayList<>(sites);
        }

        public List<Site> getAllInOrder() {
            Map<Integer, Integer> ordering = getOrdering();

            List<Site> ordered = new ArrayList<>(sites);
            Collections.sort(ordered,
                    (lhs, rhs) -> ordering.get(lhs.id()) - ordering.get(rhs.id()));

            return ordered;
        }

        private void addAll(List<Site> all) {
            List<Site> copy = new ArrayList<>(sites);
            copy.addAll(all);
            resetSites(copy);
            setChanged();
        }

        private void add(Site site) {
            List<Site> copy = new ArrayList<>(sites);
            copy.add(site);
            resetSites(copy);
            setChanged();
        }

        // We don't keep the order ourselves here, that's the task of listeners. Do notify the
        // listeners.
        private void wasReordered() {
            setChanged();
        }

        private void resetSites(List<Site> newSites) {
            sites = Collections.unmodifiableList(newSites);
            SparseArray<Site> byId = new SparseArray<>(newSites.size());
            for (Site newSite : newSites) {
                byId.put(newSite.id(), newSite);
            }
            sitesById = byId;
        }
    }

    private class SiteConfigSettingsHolder {
        Site site;
        SiteConfig config;
        JsonSettings settings;

        public SiteConfigSettingsHolder(Site site, SiteConfig config, JsonSettings settings) {
            this.site = site;
            this.config = config;
            this.settings = settings;
        }
    }

    // very dirty hack to allow the user to remove invalid sites
    public static class InvalidSite extends Lainchan {

        @Override
        public void setup() {
            super.setup();
            setName("INVALID SITE");
            setIcon(SiteIcon.fromFavicon(HttpUrl.parse("https://s.4cdn.org/image/favicon-status.ico")));
        }
    }
}
