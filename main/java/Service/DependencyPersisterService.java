package Service;

import Model.Dependency;
import Service.Repository.DependencyRepository;

import java.util.HashSet;
import java.util.List;

public class DependencyPersisterService {

    public static boolean updateProjectDependencies(int projectId, List<Dependency> newDeps) {
        List<Dependency> oldDeps = DependencyRepository.getForProject(projectId);
        if (!new HashSet<>(newDeps).equals(new HashSet<>(oldDeps))) {
            DependencyRepository.replaceForProject(projectId, newDeps);
            System.out.println("Závislosti boli aktualizované v databáze.");
            return true;
        } else {
            System.out.println("Závislosti sa nezmenili – databáza ostáva rovnaká.");
            return false;
        }
    }
}
