from __future__ import annotations


DEFAULT_ENTITY_DICTIONARY: dict[str, list[str]] = {
    "Disease": ["龋病", "龋齿", "早期龋", "中龋", "深龋"],
    "ImagingFinding": ["透射影", "脱矿", "邻面龋影", "咬合面龋影", "牙釉质缺损"],
    "Severity": ["低风险", "中风险", "高风险", "轻度", "中度", "重度"],
    "RiskFactor": ["高糖饮食", "夜奶", "口腔卫生差", "菌斑", "窝沟深", "低氟", "频繁进食"],
    "Recommendation": ["定期复查", "窝沟封闭", "局部涂氟", "控制糖摄入", "规范刷牙", "及时复诊"],
    "Population": ["儿童", "学龄前儿童", "青少年", "成人", "孕妇"],
    "AgeGroup": ["婴幼儿", "学龄前", "学龄期", "青少年", "成人"],
    "Contraindication": ["不耐受", "禁忌", "不建议"],
    "Guideline": ["指南", "共识", "手册", "规范"],
    "ClinicalAction": ["观察", "复诊", "涂氟", "封闭", "修复"],
}
