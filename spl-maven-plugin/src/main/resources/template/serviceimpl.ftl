package ${serverImplPackage};

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ${entityImport};
import ${mapperImport};
import ${serverImport};

/**
* 服务实现类
*
* @author surpassliang
* @version 1.0
* @date ${dateStr}
*/
@Service
@Transactional(rollbackFor = Exception.class)
public class ${serverImplName} extends ServiceImpl<${mapperName}, ${entityName}>
        implements ${serverName} {

}
