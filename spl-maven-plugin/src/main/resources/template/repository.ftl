package ${repositoryPackage};

import ${entityImport};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
* 仓库
*
* @author surpassliang
* @version 1.0
* @date ${dateStr}
*/
@Repository
public interface ${repositoryName}
extends JpaRepository<${entityName}, Long>, JpaSpecificationExecutor<${entityName}> {

}
