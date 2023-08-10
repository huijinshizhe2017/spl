package com.surpass.spl.plugin;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * mybatis插件
 *
 * @author surpassliang
 * @date 2023/8/10 13:22
 */
@Mojo(name = "mbp", defaultPhase = LifecyclePhase.VALIDATE)
public class MybatisPlugin extends AbstractMojo {

    @Parameter(name = "basePackage", required = true)
    private String basePackage;

    @Parameter(name = "include",defaultValue = "")
    private String include;

    @Parameter(name = "conRep", defaultValue = "false")
    private Boolean conRep;

    @Parameter(name = "entityName", defaultValue = "entity")
    private String entityName;

    @Parameter(name = "mapperName", defaultValue = "mapper")
    private String mapperName;

    @Parameter(name = "serviceName", defaultValue = "service")
    private String serviceName;

    @Parameter(name = "serviceImplName", defaultValue = "impl")
    private String serviceImplName;

    @Parameter(name = "repositoryName", defaultValue = "repository")
    private String repositoryName;


    @Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
    private File srcDic;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File target;

    @Override
    public void execute() {

        List<String> includeList = parseInclude();

        //basePath
        String basePath = srcDic.getAbsolutePath() + File.separator +basePackage.replace(".",File.separator);
        //entity
        String entityPath = basePath + File.separator + entityName;
        String entityPackage = basePackage + "." + entityName;

        //repository
        Pair<String, String> repositoryPair = null;
        if (Boolean.TRUE.equals(conRep)) {
            repositoryPair = generatorPackAndPath(basePath, basePackage, repositoryName);
        }
        //mapper
        Pair<String, String> mapperPair = generatorPackAndPath(basePath, basePackage, mapperName);
        //service
        Pair<String, String> servicePair = generatorPackAndPath(basePath, basePackage, serviceName);
        //serviceImpl
        Pair<String, String> serviceImplPair = generatorPackAndPath(servicePair.getKey(), servicePair.getValue(), serviceImplName);

        File entityFile = new File(entityPath);
        for (File file : Objects.requireNonNull(entityFile.listFiles())) {
            String entityWithSufName = file.getName();
            if (file.isDirectory() || !entityWithSufName.endsWith(".java")) {
                continue;
            }
            String entityFileName = entityWithSufName.substring(0, entityWithSufName.length() - 5);
            if (!includeList.isEmpty() && !includeList.contains(entityFileName)) {
                continue;
            }

            String preName = entityFileName.endsWith("Entity") ? entityFileName.substring(0, entityFileName.length() - 6) : entityFileName;

            //entity
            Map<String, String> param = new HashMap<>(12);
            param.put("entityName", entityFileName);
            param.put("entityImport", entityPackage + "." + entityFileName);

            //repositoryPair
            File repositoryFile = null;
            if(repositoryPair != null) {
                repositoryFile = fillParam(param, "repository", repositoryPair, preName + "Repository");
                if (repositoryFile == null) {
                    continue;
                }
            }

            // mapper
            File mapperFile = fillParam(param, "mapper", mapperPair, preName + "Mapper");
            if (mapperFile == null) {
                continue;
            }

            //service
            File serverFile = fillParam(param, "server", servicePair, "I" + preName + "Service");
            if (serverFile == null) {
                continue;
            }

            //serverImpl
            File serverImplFile = fillParam(param, "serverImpl", serviceImplPair, preName + "ServiceImpl");
            if (serverImplFile == null) {
                continue;
            }

            //时间
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            param.put("dateStr", simpleDateFormat.format(new Date()));

            getLog().info("类名为" + entityFileName + "的相关类正在生成...");
            generator("mapper.ftl", mapperFile, param);
            getLog().info("已生成Mapper映射类===>" + mapperFile.getName());
            generator("iservice.ftl", serverFile, param);
            getLog().info("已生成服务接口类===>" + serverFile.getName());
            generator("serviceimpl.ftl", serverImplFile, param);
            getLog().info("已生成服务实现类===>" + serverImplFile.getName());
            if (Boolean.TRUE.equals(conRep) && repositoryFile != null) {
                generator("repository.ftl", repositoryFile, param);
                getLog().info("已生成仓库类===>" + repositoryFile.getName());
            }
        }
    }


    private List<String> parseInclude() {
        String includeName = System.getProperty("in");
        includeName = StringUtils.isEmpty(includeName)?include:includeName;
        List<String> includeList = new ArrayList<>();
        if (!StringUtils.isEmpty(includeName)) {
            String[] split = includeName.split(",");
            includeList.addAll(Arrays.asList(split));
        }
        return includeList;
    }


    private File fillParam(Map<String, String> param, String type, Pair<String, String> pair, String name) {
        File tempFile = new File(pair.getKey(), name + ".java");
        if (tempFile.exists()) {
            return null;
        }
        //mapper
        param.put(type + "Package", pair.getValue());
        param.put(type + "Name", name);
        param.put(type + "Import", pair.getValue() + "." + name);
        return tempFile;
    }


    /**
     * 生成包及路径
     *
     * @param basePath 基本路径
     * @param name     名称
     * @return 包名和路径
     */
    private Pair<String, String> generatorPackAndPath(String basePath, String basePackage, String name) {
        //mapper
        String path = basePath + File.separator + name;
        String packageStr = basePackage + "." + name;
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdir();
        }
        return Pair.of(path, packageStr);
    }


    private void generator(String ftlName, File outFile, Map<String, String> param) {
        try {
            Configuration cfg = new Configuration();
            cfg.setClassForTemplateLoading(this.getClass(), "/template");
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            Template t = cfg.getTemplate(ftlName);
            Writer out = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8);
            t.process(param, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getMapperName() {
        return mapperName;
    }

    public void setMapperName(String mapperName) {
        this.mapperName = mapperName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceImplName() {
        return serviceImplName;
    }

    public void setServiceImplName(String serviceImplName) {
        this.serviceImplName = serviceImplName;
    }

    public File getSrcDic() {
        return srcDic;
    }

    public void setSrcDic(File srcDic) {
        this.srcDic = srcDic;
    }

    public File getTarget() {
        return target;
    }

    public void setTarget(File target) {
        this.target = target;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public Boolean getConRep() {
        return conRep;
    }

    public void setConRep(Boolean conRep) {
        this.conRep = conRep;
    }

    public String getInclude() {
        return include;
    }

    public void setInclude(String include) {
        this.include = include;
    }
}
