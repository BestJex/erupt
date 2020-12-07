package xyz.erupt.core.util;

import org.apache.commons.lang3.StringUtils;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.core.service.EruptCoreService;
import xyz.erupt.core.view.EruptFieldModel;
import xyz.erupt.core.view.EruptModel;
import xyz.erupt.core.view.TreeModel;

import java.util.*;

/**
 * @author liyuepeng
 * @date 2019-04-28.
 */
public class DataHandlerUtil {
    //内存计算的方式生成树结构
    public static List<TreeModel> treeModelToTree(List<TreeModel> treeModels) {
        List<TreeModel> resultTreeModels = new ArrayList<>();
        List<TreeModel> tempTreeModels = new LinkedList<>();
        tempTreeModels.addAll(treeModels);
        for (TreeModel treeModel : treeModels) {
            if (treeModel.isRoot()) {
                resultTreeModels.add(treeModel);
                tempTreeModels.remove(treeModel);
            }
        }
        for (TreeModel treeModel : resultTreeModels) {
            recursionTree(tempTreeModels, treeModel);
        }
        return resultTreeModels;
    }

    private static void recursionTree(List<TreeModel> treeModels, TreeModel parentTreeModel) {
        List<TreeModel> childrenModel = new ArrayList<>();
        List<TreeModel> tempTreeModels = new LinkedList<>();
        tempTreeModels.addAll(treeModels);
        for (TreeModel treeModel : treeModels) {
            if (null != treeModel.getPid() && treeModel.getPid().equals(parentTreeModel.getId())) {
                childrenModel.add(treeModel);
                tempTreeModels.remove(treeModel);
                if (childrenModel.size() > 0) {
                    recursionTree(tempTreeModels, treeModel);
                }
                parentTreeModel.setChildren(childrenModel);
            }
        }
    }


    public static void convertDataToEruptView(EruptModel eruptModel, Collection<Map<String, Object>> list) {
        Map<String, Map<String, String>> choiceItems = new HashMap<>();
        for (Map<String, Object> map : list) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                if (entry.getKey().contains("_")) {
                    key = entry.getKey().split("_")[0];
                }
                EruptFieldModel fieldModel = eruptModel.getEruptFieldMap().get(key);
                Edit edit = fieldModel.getEruptField().edit();
                switch (edit.type()) {
                    case REFERENCE_TREE:
                    case REFERENCE_TABLE:
                    case COMBINE:
                        String[] _keys = entry.getKey().split("_");
                        for (View view : fieldModel.getEruptField().views()) {
                            if (view.column().equals(_keys[_keys.length - 1])) {
                                EruptFieldModel vef = EruptCoreService.getErupt(fieldModel.getFieldReturnName()).
                                        getEruptFieldMap().get(view.column());
                                map.put(entry.getKey(), convertColumnValue(vef, entry.getValue(), choiceItems));
                            }
                        }
                        break;
                    default:
                        map.put(entry.getKey(), convertColumnValue(fieldModel, entry.getValue(), choiceItems));
                        break;
                }
            }
        }
    }

    private static Object convertColumnValue(EruptFieldModel fieldModel, Object value, Map<String, Map<String, String>> choiceItems) {
        if (null == value) {
            return null;
        }
        Edit edit = fieldModel.getEruptField().edit();
        switch (edit.type()) {
            case DATE:
                if (edit.dateType().type() == DateType.Type.DATE) {
                    if (StringUtils.isNotBlank(value.toString())) {
                        return value.toString().substring(0, 10);
                    }
                }
                break;
            case CHOICE:
                Map<String, String> cm = choiceItems.get(fieldModel.getFieldName());
                if (null == cm) {
                    cm = EruptUtil.getChoiceMap(edit.choiceType());
                    choiceItems.put(fieldModel.getFieldName(), cm);
                }
                return cm.get(value.toString());
            case BOOLEAN:
                return (Boolean) value ? edit.boolType().trueText() : edit.boolType().falseText();
        }
        return value;
    }


}