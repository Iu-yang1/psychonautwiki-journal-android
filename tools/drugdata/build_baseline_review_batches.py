#!/usr/bin/env python3
"""Generate reviewable baseline-substance batches from local community data.

The generated batches preserve the existing roas.dose and roas.duration data.
They do not turn community dose ranges into clinical dose references, and they
do not infer approved indications from a PsychonautWiki identity page.
"""

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "tools/drugdata"
DEFAULT_OUTPUT = ROOT / "build/library-sources"
TODAY = "2026-11-06"
FIELDS = (
    "identity",
    "commonNames",
    "categories",
    "roas",
    "summary",
    "clinicalInfo.indications",
    "clinicalInfo.contraindications",
    "clinicalInfo.majorWarnings",
    "clinicalInfo.majorInteractions",
    "clinicalInfo.monitoring",
    "doseUseReferences",
    "timeCourse",
    "sourceRefs",
    "translation",
    "interactions",
)

CATEGORY_ZH = {
    "barbiturate": "巴比妥类",
    "benzodiazepine": "苯二氮䓬类",
    "cannabinoid": "大麻素类",
    "common": "常见",
    "deliriant": "谵妄剂",
    "depressant": "抑制剂",
    "dissociative": "分离剂",
    "entactogen": "同感剂",
    "eugeroic": "促醒剂",
    "habit-forming": "习惯性使用",
    "hallucinogen": "致幻剂",
    "nootropic": "促智药",
    "nitrite": "亚硝酸酯类",
    "opioid": "阿片类",
    "psychedelic": "迷幻剂",
    "research-chemical": "新型精神活性物质",
    "stimulant": "兴奋剂",
    "tentative": "分类待定",
}

CATEGORY_EN = {
    "barbiturate": "barbiturate",
    "benzodiazepine": "benzodiazepine",
    "cannabinoid": "cannabinoid",
    "common": "common",
    "deliriant": "deliriant",
    "depressant": "depressant",
    "dissociative": "dissociative",
    "entactogen": "entactogen",
    "eugeroic": "eugeroic",
    "habit-forming": "habit-forming",
    "hallucinogen": "hallucinogen",
    "nootropic": "nootropic",
    "nitrite": "alkyl nitrite",
    "opioid": "opioid",
    "psychedelic": "psychedelic",
    "research-chemical": "novel psychoactive substance",
    "stimulant": "stimulant",
    "tentative": "tentative classification",
}

CATEGORY_PRIORITY = (
    "opioid",
    "benzodiazepine",
    "barbiturate",
    "depressant",
    "stimulant",
    "psychedelic",
    "dissociative",
    "cannabinoid",
    "deliriant",
    "hallucinogen",
    "entactogen",
    "nootropic",
    "eugeroic",
    "nitrite",
)

PRIMARY_CATEGORY_OVERRIDES = {
    "Benzydamine": "deliriant",
    "Cannabis": "cannabinoid",
    "Datura": "deliriant",
    "Diphenhydramine": "deliriant",
    "MDA": "entactogen",
    "MDEA": "entactogen",
    "MDMA": "entactogen",
    "Nicotine": "stimulant",
    "PMA": "entactogen",
    "PMMA": "entactogen",
    "Poppers": "nitrite",
    "Promethazine": "deliriant",
    "Salvinorin A": "hallucinogen",
    "Tilidin": "opioid",
}

NAME_ZH = {
    "1,4-Butanediol": "1,4-丁二醇",
    "2,5-DMA": "2,5-二甲氧基安非他明",
    "2-Aminoindane": "2-氨基茚",
    "2-Fluorodeschloroketamine": "2-氟脱氯氯胺酮",
    "2M2B": "2-甲基-2-丁醇",
    "4-AcO-DMT": "4-乙酰氧基-DMT",
    "5-Hydroxytryptophan": "5-羟色氨酸",
    "5-MeO-DMT": "5-甲氧基-DMT",
    "Acetylfentanyl": "乙酰芬太尼",
    "Adrafinil": "阿屈非尼",
    "Alcohol": "酒精",
    "Allylescaline": "烯丙基麦司卡林",
    "Alpha-GPC": "α-甘油磷酸胆碱",
    "Aniracetam": "阿尼西坦",
    "Apomorphine": "阿扑吗啡",
    "Ayahuasca": "死藤水",
    "Baclofen": "巴氯芬",
    "Benzydamine": "苄达明",
    "Bromantane": "溴坦",
    "Bromazolam": "溴唑仑",
    "Bromo-DragonFLY": "溴龙蝇",
    "Bufotenin": "蟾毒色胺",
    "Butylone": "丁基酮",
    "Caffeine": "咖啡因",
    "Cannabidiol": "大麻二酚",
    "Cannabis": "大麻",
    "Carisoprodol": "卡立普多",
    "Choline bitartrate": "酒石酸胆碱",
    "Cinolazepam": "西诺西泮",
    "Citicoline": "胞磷胆碱",
    "Clonazolam": "氯硝唑仑",
    "Cocaine": "可卡因",
    "Codeine": "可待因",
    "Coluracetam": "科鲁拉西坦",
    "Creatine": "肌酸",
    "Cyclazodone": "环唑酮",
    "DET": "二乙基色胺",
    "DMT": "N,N-二甲基色胺",
    "DPT": "二丙基色胺",
    "Datura": "曼陀罗",
    "Deschloroetizolam": "去氯依替唑仑",
    "Deschloroketamine": "去氯氯胺酮",
    "Desomorphine": "地索吗啡",
    "Desoxypipradrol": "去氧哌甲酯",
    "Dextromethorphan": "右美沙芬",
    "Dextropropoxyphene": "右丙氧芬",
    "DiPT": "二异丙基色胺",
    "Diclazepam": "二氯西泮",
    "Dihydrocodeine": "双氢可待因",
    "Diphenhydramine": "苯海拉明",
    "Diphenidine": "二苯乙基哌啶",
    "EPT": "乙基丙基色胺",
    "ETH-CAT": "乙基卡西酮",
    "Efavirenz": "依非韦伦",
    "Ephedrine": "麻黄碱",
    "Ephenidine": "依苯尼定",
    "Ephylone": "乙基酮",
    "Escaline": "艾司卡林",
    "Ethylmorphine": "乙基吗啡",
    "Ethylone": "乙基酮",
    "Ethylphenidate": "乙基哌甲酯",
    "Etomidate": "依托咪酯",
    "F-Phenibut": "氟苯尼布特",
    "Fenethylline": "芬乙茶碱",
    "Fentanyl": "芬太尼",
    "Flualprazolam": "氟阿普唑仑",
    "Flubromazepam": "氟溴西泮",
    "Flubromazolam": "氟溴唑仑",
    "Flunitrazolam": "氟硝唑仑",
    "GBL": "γ-丁内酯",
    "GHB": "γ-羟基丁酸",
    "Gaboxadol": "加波沙朵",
    "Heroin": "海洛因",
    "Hexedrone": "己酮",
    "Hydrocodone": "氢可酮",
    "Hydromorphone": "氢吗啡酮",
    "Ibogaine": "伊博格碱",
    "Isopropylphenidate": "异丙基哌甲酯",
    "Kratom": "卡痛",
    "LSA": "麦角酸酰胺",
    "LSD": "麦角酸二乙酰胺",
    "MDA": "3,4-亚甲二氧基安非他明",
    "MDMA": "3,4-亚甲二氧基甲基安非他明",
    "MDPV": "亚甲二氧基吡咯烷酮",
    "Meclofenoxate": "氯酯醒",
    "Mephedrone": "4-甲基甲卡西酮",
    "Mephenaqualone": "甲苯喹唑酮",
    "Mescaline": "麦司卡林",
    "Methallylescaline": "甲烯丙基麦司卡林",
    "Methamphetamine": "甲基苯丙胺",
    "Methaqualone": "甲喹酮",
    "Methcathinone": "甲卡西酮",
    "Methiopropamine": "甲硫丙胺",
    "Methoxetamine": "甲氧西他明",
    "Methoxphenidine": "甲氧苯尼定",
    "Methylnaphthidate": "甲基萘哌甲酯",
    "Methylone": "亚甲二氧基甲卡西酮",
    "Metizolam": "美替唑仑",
    "Mexedrone": "甲氧麻黄酮",
    "Morphine": "吗啡",
    "Myristicin": "肉豆蔻醚",
    "N-Ethylhexedrone": "N-乙基己酮",
    "N-Methylbisfluoromodafinil": "N-甲基双氟莫达非尼",
    "NEP": "N-乙基戊酮",
    "Nicotine": "尼古丁",
    "Nifoxipam": "尼氟西泮",
    "Nitrous": "一氧化二氮",
    "O-Desmethyltramadol": "O-去甲基曲马多",
    "Omberacetam": "奥贝拉西坦",
    "Oxiracetam": "奥拉西坦",
    "Oxycodone": "羟考酮",
    "Oxymorphone": "羟吗啡酮",
    "PCP": "苯环己哌啶",
    "Pentedrone": "戊酮",
    "Pethidine": "哌替啶",
    "Phenazepam": "酚那西泮",
    "Phenibut": "苯尼布特",
    "Phenylpiracetam": "苯基吡拉西坦",
    "Piracetam": "吡拉西坦",
    "Poppers": "亚硝酸酯类",
    "Pramiracetam": "普拉西坦",
    "Progesterone": "黄体酮",
    "Prolintane": "普罗林坦",
    "Promethazine": "异丙嗪",
    "Propylhexedrine": "丙己君",
    "Proscaline": "丙基麦司卡林",
    "Psilocin": "裸盖菇素",
    "Psilocybin mushrooms": "裸盖菇",
    "Pyrazolam": "吡唑仑",
    "Rolicyclidine": "罗利环啶",
    "SAM-e": "S-腺苷甲硫氨酸",
    "Salvinorin A": "鼠尾草素 A",
    "Secobarbital": "司可巴比妥",
    "Sufentanil": "舒芬太尼",
    "Tapentadol": "他喷他多",
    "Theacrine": "茶立碱",
    "Theanine": "茶氨酸",
    "Tilidin": "替利定",
    "Tramadol": "曲马多",
    "Tyrosine": "酪氨酸",
    "αMT": "α-甲基色胺",
}

SUMMARY_ZH_OVERRIDES = {
    "JWH-018": "JWH-018 是一种合成大麻素受体激动剂，属于新型精神活性物质；现有资料主要来自社区报告，人体药理与长期安全性证据有限。",
    "Apomorphine": "阿扑吗啡是一种多巴胺受体激动剂，临床用于特定运动障碍等；本条目保留社区资料身份，不据此提供适应证或剂量建议。",
    "F-Phenibut": "氟苯尼布特是苯尼布特的氟化类似物，属于中枢抑制剂；社区资料提示其作用与苯尼布特相近，但人体药代与安全性资料有限。",
    "Fenethylline": "芬乙茶碱是安非他明与茶碱的衍生物，属于兴奋剂；现有资料有限，不应据社区描述自行使用。",
    "N-Methylbisfluoromodafinil": "N-甲基双氟莫达非尼是莫达非尼类促醒剂的新型精神活性物质；人体药代、剂量与长期安全性资料有限。",
    "Nitrous": "一氧化二氮（笑气）是一种吸入性麻醉/分离性气体；短期可致缺氧、头晕和意识改变，长期滥用与神经损伤相关。",
    "Theanine": "茶氨酸是茶叶中的氨基酸，常作为膳食补充剂；现有证据不支持将其作为药物治疗，具体剂量与相互作用需谨慎。",
    "4F-MPH": "4-氟哌甲酯是哌甲酯的氟化类似物，属于兴奋剂类新型精神活性物质；人体药代与长期安全性资料有限。",
    "Etomidate": "依托咪酯是一种静脉麻醉药；非医疗使用可导致意识丧失、呼吸抑制和肾上腺功能抑制等严重风险。",
    "THJ-2201": "THJ-2201 是一种合成大麻素类新型精神活性物质；其效力与毒性资料有限，社区报告提示高剂量风险显著。",
    "Phenazepam": "酚那西泮是一种长效、强效苯二氮䓬类；剂量难以把握，社区资料提示其易导致过度镇静、遗忘和依赖。",
    "5-Hydroxytryptophan": "5-羟色氨酸是血清素前体，常作为膳食补充剂；与血清素能药物合用可能增加 5-羟色胺综合征风险。",
    "Desoxypipradrol": "去氧哌甲酯是一种长时效兴奋剂类新型精神活性物质；社区资料提示其作用持续时间长，易导致失眠、心血管负担和精神症状。",
    "A-PHP": "A-PHP 是一种吡咯烷酮类兴奋剂新型精神活性物质；可导致心血管负担、焦虑、失眠和依赖风险。",
    "Creatine": "肌酸是一种常见的运动补充剂；现有资料主要支持其运动表现用途，长期大剂量与肾脏疾病人群的安全性需谨慎评估。",
    "THJ-018": "THJ-018 是一种合成大麻素类新型精神活性物质；人体药理、剂量和长期安全性资料有限。",
    "Tilidin": "替利定是一种阿片类镇痛药（常与纳洛酮复方）；可导致呼吸抑制、镇静和依赖，非医疗使用风险高。",
    "STS-135": "STS-135 是一种合成大麻素类新型精神活性物质；其效力和毒性资料有限，社区报告提示高剂量风险。",
    "JWH-073": "JWH-073 是一种合成大麻素类新型精神活性物质；人体药理与长期安全性资料有限。",
    "N-Ethylhexedrone": "N-乙基己酮是一种卡西酮类兴奋剂新型精神活性物质；可导致心血管负担、焦虑、失眠和依赖风险。",
    "A-PVP": "A-PVP 是一种吡咯烷酮类兴奋剂新型精神活性物质；可导致心血管负担、精神症状和依赖风险。",
    "SAM-e": "S-腺苷甲硫氨酸是一种参与甲基化反应的膳食补充剂；与血清素能药物合用可能增加 5-羟色胺综合征风险。",
    "Cinolazepam": "西诺西泮是一种苯二氮䓬类，主要用于催眠；可导致镇静、遗忘、呼吸抑制和依赖。",
    "Methiopropamine": "甲硫丙胺是一种安非他明类兴奋剂新型精神活性物质；人体药代与长期安全性资料有限。",
    "Myristicin": "肉豆蔻醚是肉豆蔻中的芳香醚类物质；高剂量可致幻觉、恶心和心血管风险，现有临床资料有限。",
    "MiPLA": "MiPLA 是一种麦角酰胺类迷幻剂新型精神活性物质；人体药理与长期安全性资料有限。",
    "2M2B": "2-甲基-2-丁醇是一种叔醇类中枢抑制剂；其效力与毒性资料有限，非医疗使用风险高。",
    "NEP": "NEP 是一种卡西酮类兴奋剂新型精神活性物质；可导致心血管负担、焦虑、失眠和依赖风险。",
    "Methcathinone": "甲卡西酮是一种卡西酮类兴奋剂；可导致心血管负担、焦虑、失眠和依赖，长期使用与神经毒性相关。",
    "4F-EPH": "4F-EPH 是乙基哌甲酯的氟化类似物，属于兴奋剂类新型精神活性物质；人体药代与长期安全性资料有限。",
    "Progesterone": "黄体酮是一种天然孕激素，临床用于黄体支持、激素替代等；具体适应证、剂量与监测需依据制剂和说明书。",
    "Desomorphine": "地索吗啡是一种强效阿片类；可导致呼吸抑制、镇静和依赖，非医疗使用风险极高。",
    "Gaboxadol": "加波沙朵是一种 GABA-A 受体激动剂新型精神活性物质；可导致镇静、意识改变和呼吸抑制风险。",
    "5-MeO-DiBF": "5-MeO-DiBF 是一种色胺类迷幻剂新型精神活性物质；人体药理与长期安全性资料有限。",
    "HXE": "HXE 是一种芳基环己胺类分离性新型精神活性物质；可导致意识改变、心血管负担和受伤风险。",
    "Bromazolam": "溴唑仑是一种苯二氮䓬类新型精神活性物质，结构与阿普唑仑相近；可导致镇静、遗忘、呼吸抑制和依赖。",
    "Dichloropane": "二氯帕烷是一种可卡因类兴奋剂新型精神活性物质；可导致心血管负担、焦虑和依赖风险。",
    "Methoxetamine": "甲氧西他明是一种分离性新型精神活性物质；可导致意识改变、心血管负担、膀胱症状和受伤风险。",
    "Methylnaphthidate": "甲基萘哌甲酯是一种哌甲酯类兴奋剂新型精神活性物质；人体药代与长期安全性资料有限。",
    "Cyclazodone": "环唑酮是一种兴奋剂类新型精神活性物质；人体药代、剂量和长期安全性资料有限。",
}

TOXICITY_ZH = {
    "exact toxic dosage is unknown": "确切中毒剂量未知",
    "toxic dose is unknown": "中毒剂量未知",
    "toxic dosage is unknown": "中毒剂量未知",
    "low toxicity": "毒性较低",
    "extremely low toxicity": "毒性极低",
    "extremely low toxicity relative to dose": "相对剂量而言毒性极低",
    "potentially lethal when mixed with depressants like alcohol or benzodiazepines": "与酒精或苯二氮䓬类等抑制剂合用可能致命",
    "potentially lethal when mixed with depressants like alcohol or opioids": "与酒精或阿片类等抑制剂合用可能致命",
    "potentially lethal when mixed with depressants like alcohol, benzodiazepines or opioids": "与酒精、苯二氮䓬类或阿片类等抑制剂合用可能致命",
    "potentially fatal at heavy dosages": "高剂量可能致命",
    "fatal at heavy dosages": "高剂量可致命",
    "no negative health effects attributed to trying this drug": "社区资料未报告尝试该物质导致的负面健康效应",
    "safe even when high doses are consumed for a long period of time": "长期高剂量使用也未见明显安全性问题（社区资料）",
    "can be considered extremely toxic when compared to other substances such as methamphetamine or mdma": "与甲基苯丙胺或 MDMA 等物质相比可视为毒性极高",
    "high toxicity": "毒性高",
    "moderate toxicity": "中等毒性",
    "fatal at amounts close to or exceeding 2 grams": "接近或超过 2 克可能致命",
    "ingestion of pma has been associated with severe tachycardia (abnormally high heart rate), seizures, hyperthermia, and death": "摄入 PMA 与严重心动过速、癫痫发作、高热和死亡相关",
    "high toxicity relative to its dose due to its extreme potency": "因效力极强，相对剂量而言毒性高",
    "large therapeutic index and margin of safety": "治疗指数和安全边际较大",
    "may cause liver toxicity at high doses or after chronic use": "高剂量或长期使用可能导致肝毒性",
    "ld50 is around 50 times a regular dose": "LD50 约为常规剂量的 50 倍",
    "unintentionally spilling a very small amount of acetylfentanyl on one's skin could result in a fatal overdose": "少量乙酰芬太尼意外接触皮肤即可导致致命过量",
    "death from ethanol consumption is possible when blood alcohol levels reach 0.4%": "血酒精浓度达到 0.4% 时可能因饮酒死亡",
    "individuals have died from as little as 30 mg applied to mucous membranes, whereas addicts may tolerate up to 5 grams daily": "有个体仅因黏膜接触 30 mg 即死亡，而依赖者每日可耐受高达 5 克",
    "known to cause potentially fatal heart arrhythmias": "已知可导致潜在致命的心律失常",
    "well-tolerated and shows little to no toxicity": "耐受性良好，几乎无明显毒性",
    "is neurotoxic": "具有神经毒性",
    "can be fatal in extremely high doses": "极高剂量可致命",
    "considered to be a safe and non-toxic substance when used responsibly or medically": "在负责任或医疗使用下被视为安全且无明显毒性",
    "doses above 10 grams are associated with a risk of death": "超过 10 克的剂量与死亡风险相关",
    "doses of over 300mg can be dangerous for first time users": "首次使用者超过 300 mg 可能危险",
    "safe at appropriate dosages": "适当剂量下安全",
    "the ld50 of caffeine in humans is dependent on individual sensitivity, but is estimated to be about 150 to 200 milligrams per kilogram of body mass or roughly 80 to 100 cups of coffee for an average adult": "咖啡因人体 LD50 因个体敏感性而异，估计约为每公斤体重 150–200 mg",
    "extreme vasoconstrictive effects": "极强的血管收缩作用",
    "one can easily overdose if this substance is not measured correctly": "未准确计量时容易过量",
    "ingestion of pmma has been associated with severe tachycardia (abnormally high heart rate), seizures, hyperthermia, and death": "摄入 PMMA 与严重心动过速、癫痫发作、高热和死亡相关",
    "unintentionally transporting the substance from the skin by touching the mouth, nose or eyes is dangerous": "皮肤接触后触摸口、鼻或眼睛可能造成危险转移",
    "potentially lethal": "可能致命",
    "moderate potential toxicity relative to its dose due to its potency": "因效力较强，相对剂量具有中等毒性潜力",
}

ADDICTION_ZH = {
    "not habit-forming": "通常不形成习惯性使用",
    "moderately addictive with a high potential for abuse": "中等成瘾性，滥用潜力高",
    "extremely physically and psychologically addictive": "身体和心理依赖性极强",
    "non-addictive with a low abuse potential": "不成瘾，滥用潜力低",
    "extremely addictive with a high potential for abuse": "成瘾性极强，滥用潜力高",
    "non-addictive with a low potential for abuse": "不成瘾，滥用潜力低",
    "highly addictive with a high potential for adverse side effects such as psychosis": "成瘾性高，且精神病性症状等不良反应风险高",
    "moderately physically and psychologically addictive": "中等身体和心理依赖性",
    "not addictive with a low potential for abuse": "不成瘾，滥用潜力低",
    "highly addictive with a high potential for abuse": "成瘾性高，滥用潜力高",
    "moderately habit-forming": "中等习惯性使用风险",
    "extremely addictive": "成瘾性极强",
    "low potential for abuse and dependence": "滥用和依赖潜力低",
    "not habit-forming with a low potential for abuse": "通常不形成习惯性使用，滥用潜力低",
    "produces dependence with chronic use": "长期使用可产生依赖",
    "mildly addictive with a low potential for abuse": "轻度成瘾性，滥用潜力低",
    "somewhat habit-forming with a low potential for abuse": "有一定习惯性使用风险，滥用潜力低",
    "produces dependence with chronic use and has a low abuse potential": "长期使用可产生依赖，滥用潜力低",
    "produces dependence with chronic use and has moderate abuse potential": "长期使用可产生依赖，滥用潜力中等",
    "mildly addictive with a moderate potential for abuse": "轻度成瘾性，滥用潜力中等",
    "not known to be habit-forming": "目前未知是否形成习惯性使用",
    "moderately addictive with a high potential for adverse side effects such as psychosis": "中等成瘾性，精神病性症状等不良反应风险高",
    "mildly habit forming": "轻度习惯性使用风险",
    "moderately addictive with a moderate potential for adverse side effects such as psychosis": "中等成瘾性，精神病性症状等不良反应风险中等",
    "produces dependence with chronic use and has a high abuse potential": "长期使用可产生依赖，滥用潜力高",
    "produces dependence with chronic use and has a high potential for abuse": "长期使用可产生依赖，滥用潜力高",
    "does not produce dependence and has low abuse potential": "不产生依赖，滥用潜力低",
    "low abuse potential": "滥用潜力低",
    "is not known to be addictive": "目前未知是否成瘾",
    "moderately to highly physically and moderately psychologically addictive": "身体依赖性中等至高度，心理依赖性中等",
}

TOLERANCE_FULL_ZH = {
    "with prolonged and repeated use": "长期反复使用后出现",
    "almost immediately after ingestion": "摄入后几乎立即出现",
    "develops with prolonged and repeated use": "长期反复使用后出现",
    "within a couple of days of continuous use": "连续使用数日后出现",
    "with repeated use": "反复使用后出现",
    "after prolonged and repeated usage": "长期反复使用后出现",
    "rapidly develops with prolonged and repeated use": "长期反复使用后迅速出现",
    "within a couple of days of repeated administration": "反复给药数日后出现",
    "within several days of continuous use": "连续使用数日后出现",
    "within 3-4 days of continuous use": "连续使用 3–4 日后出现",
    "within several weeks of continuous use": "连续使用数周后出现",
    "after ingestion over the couse of multiple days": "连续数日摄入后出现",
    "does not appear to occur": "似乎不会出现",
    "with prolonged use": "长期使用后出现",
    "repeated and heavy usage": "反复且大量使用后出现",
    "within a few days of repeated administration": "反复给药数日后出现",
    "develops over several weeks of prolonged and repeated use": "长期反复使用数周后出现",
    "after repeated and frequent usage": "反复频繁使用后出现",
}

DURATION_ZH = {
    "second": "秒",
    "seconds": "秒",
    "minute": "分钟",
    "minutes": "分钟",
    "hour": "小时",
    "hours": "小时",
    "day": "日",
    "days": "日",
    "week": "周",
    "weeks": "周",
    "month": "个月",
    "months": "个月",
    "year": "年",
    "years": "年",
}
DURATION_PATTERN = re.compile(
    r"(?P<range>\d+(?:\.\d+)?(?:\s*[-–]\s*\d+(?:\.\d+)?)?)\s*"
    r"(?P<unit>seconds?|minutes?|hours?|days?|weeks?|months?|years?)",
    re.IGNORECASE,
)

CROSS_ZH = {
    "psychedelic": "迷幻剂",
    "psychedelics": "迷幻剂",
    "dopamine": "多巴胺能",
    "dopaminergic": "多巴胺能",
    "stimulant": "兴奋剂",
    "stimulants": "兴奋剂",
    "opioids": "阿片类",
    "dissociative": "分离剂",
    "dissociatives": "分离剂",
    "cannabinoids": "大麻素类",
    "benzodiazepines": "苯二氮䓬类",
    "benzodiazepine": "苯二氮䓬类",
    "racetam": "拉西坦类",
    "nootropic": "促智药",
    "nootropics": "促智药",
    "gaba": "GABA 能",
    "gabaergic": "GABA 能",
    "depressants": "抑制剂",
    "serotonin": "血清素能",
    "serotonergic": "血清素能",
    "deliriant": "谵妄剂",
    "modafanil": "莫达非尼类",
    "analog": "类似物",
    "entactogens": "同感剂",
    "benzhydryl": "二苯甲基类",
    "thienzodiazepines": "噻吩二氮䓬类",
    "thienodiazepine": "噻吩二氮䓬类",
    "hallucinogen": "致幻剂",
}

CATEGORY_WARNING_ZH = {
    "stimulant": "兴奋剂类可增加心率、血压和体温，导致焦虑、失眠、心悸、胸痛、癫痫或精神病性症状；反复使用可产生耐受和依赖。",
    "depressant": "中枢抑制剂可导致镇静、共济失调、呼吸抑制和意识丧失；与酒精、阿片类或苯二氮䓬类合用时风险显著增加。",
    "opioid": "阿片类可导致呼吸抑制、过度镇静、便秘和依赖；与其他抑制剂或酒精合用可能致命，过量时需立即使用纳洛酮并就医。",
    "psychedelic": "迷幻剂可导致感知扭曲、焦虑、惊恐、偏执和精神病性症状；个体反应差异大，单一使用也可能触发潜在精神障碍。",
    "dissociative": "分离性物质可导致意识改变、解离、血压升高、受伤风险和膀胱/泌尿系统症状；长期反复使用风险更高。",
    "cannabinoid": "大麻素类可导致焦虑、心悸、血压变化、认知受损和精神症状；合成大麻素效力与毒性不确定，过量风险更高。",
    "deliriant": "谵妄剂可导致抗胆碱能毒性、意识混乱、幻觉、高热、尿潴留、癫痫和心血管事件；剂量稍高即可危及生命。",
    "nootropic": "促智/补充剂类急性风险通常较低，但证据质量不一，可能与处方药相互作用；长期高剂量安全性资料有限。",
    "hallucinogen": "致幻物质可导致感知扭曲、焦虑、惊恐和精神症状；个体反应差异大，应在安全环境中使用。",
    "entactogen": "同感剂可导致心率、血压和体温升高、低钠血症、焦虑和 5-羟色胺综合征；与血清素能药物合用风险高。",
    "benzodiazepine": "苯二氮䓬类可导致镇静、遗忘、呼吸抑制和依赖；与酒精或阿片类合用可能致命，突然停药可致戒断和癫痫。",
    "barbiturate": "巴比妥类可导致呼吸抑制、意识丧失和依赖；治疗窗窄，过量风险高。",
    "eugeroic": "促醒剂可导致失眠、焦虑、血压升高和心悸；长期使用与依赖和心血管风险相关。",
    "nitrite": "亚硝酸酯类可导致明显血管扩张、低血压、头晕和晕厥；与 PDE5 抑制剂（西地那非、他达拉非等）合用可致严重低血压，与硝酸酯类药物合用禁忌。吸入可致头痛、面部潮红和鼻黏膜刺激，罕见高铁血红蛋白血症和视网膜损伤；误服可致命。",
    "default": "社区资料提示该物质可能具有精神活性作用；急性中毒、长期健康和相互作用资料有限。",
}

CATEGORY_WARNING_EN = {
    "stimulant": "Stimulants can increase heart rate, blood pressure and body temperature and may cause anxiety, insomnia, chest pain, seizures or psychotic symptoms. Tolerance and dependence can develop with repeated use.",
    "depressant": "CNS depressants can cause sedation, ataxia, respiratory depression and loss of consciousness. Mixing with alcohol, opioids or benzodiazepines substantially increases risk.",
    "opioid": "Opioids can cause respiratory depression, excessive sedation, constipation and dependence. Mixing with alcohol or other depressants can be fatal; suspected overdose requires emergency help and naloxone where available.",
    "psychedelic": "Psychedelics can cause perceptual distortion, anxiety, panic, paranoia and psychotic symptoms. Individual reactions vary, and a single use may trigger latent mental illness.",
    "dissociative": "Dissociatives can cause altered consciousness, dissociation, raised blood pressure, injury risk and bladder/urinary symptoms. Repeated use carries additional risk.",
    "cannabinoid": "Cannabinoids can cause anxiety, palpitations, blood-pressure changes, cognitive impairment and psychiatric symptoms. Synthetic cannabinoid potency and toxicity are uncertain.",
    "deliriant": "Deliriants can cause anticholinergic toxicity, confusion, hallucinations, hyperthermia, urinary retention, seizures and cardiovascular events. Small dose increases can be life-threatening.",
    "nootropic": "Nootropic and supplement products usually have lower acute risk, but evidence quality varies and interactions with prescription medicines are possible. Long-term high-dose safety data are limited.",
    "hallucinogen": "Hallucinogens can cause perceptual distortion, anxiety, panic and psychiatric symptoms. Individual reactions vary; a safe setting is essential.",
    "entactogen": "Entactogens can raise heart rate, blood pressure and temperature and may cause hyponatremia, anxiety and serotonin syndrome. Serotonergic drug combinations carry high risk.",
    "benzodiazepine": "Benzodiazepines can cause sedation, amnesia, respiratory depression and dependence. Mixing with alcohol or opioids can be fatal, and abrupt withdrawal can cause seizures.",
    "barbiturate": "Barbiturates can cause respiratory depression, loss of consciousness and dependence. The therapeutic window is narrow and overdose risk is high.",
    "eugeroic": "Wakefulness-promoting agents can cause insomnia, anxiety, raised blood pressure and palpitations. Long-term use has been linked to dependence and cardiovascular risk.",
    "nitrite": "Alkyl nitrites cause marked vasodilation, hypotension, dizziness and syncope. Combining with PDE5 inhibitors such as sildenafil or tadalafil can cause severe hypotension, and use with nitrate medicines is contraindicated. Inhalation can cause headache, flushing and nasal irritation, and rarely methemoglobinemia or retinal injury; ingestion can be fatal.",
    "default": "Community sources describe psychoactive effects, but acute toxicity, long-term health effects and interaction data are limited.",
}

CATEGORY_MONITORING_ZH = {
    "stimulant": [
        "监测心率、血压、体温和水分摄入；避免与酒精、其他兴奋剂或 MAOI 合用。",
        "出现胸痛、高热、意识改变、癫痫或持续失眠时立即就医。",
    ],
    "depressant": [
        "监测意识、呼吸频率和体温；不要与酒精、阿片类、苯二氮䓬类或其他镇静剂合用。",
        "出现呼吸变浅、无法唤醒、呕吐或抽搐时立即求助。",
    ],
    "opioid": [
        "监测呼吸、镇静程度和瞳孔变化；身边备有纳洛酮并有人陪伴。",
        "出现呼吸变浅、嘴唇发紫或无法唤醒时立即呼叫急救。",
    ],
    "psychedelic": [
        "监测心理状态、体温和水分；在安全、熟悉的环境中使用并有人陪伴。",
        "出现持续惊恐、偏执、精神病性症状或自伤风险时寻求帮助。",
    ],
    "dissociative": [
        "监测意识、血压、体温和受伤风险；避免独自使用或与酒精/镇静剂合用。",
        "关注膀胱和泌尿系统症状，反复使用应停止并就医。",
    ],
    "cannabinoid": [
        "监测焦虑、心悸、血压和精神症状；避免驾驶或操作机械。",
        "有精神病风险或心血管疾病者应避免使用。",
    ],
    "deliriant": [
        "监测意识、体温、心率、尿潴留和抽搐；避免与抗胆碱药、酒精或镇静剂合用。",
        "出现高热、癫痫、严重激越或意识改变时立即就医。",
    ],
    "entactogen": [
        "监测体温、水分和电解质、心率和血压；避免过热、过度饮水或缺水，不要与血清素能药物或 MAOI 合用。",
        "出现高热、意识改变、肌肉僵硬、抽搐或严重头痛时立即就医。",
    ],
    "nootropic": [
        "监测睡眠、情绪、血压和消化不适；不要与处方药叠加。",
        "缺乏长期安全性资料，出现持续不适时停用并咨询专业人员。",
    ],
    "default": [
        "监测意识、呼吸、心率、血压和体温；避免混合使用。",
        "出现严重或持续症状时立即就医。",
    ],
    "nitrite": [
        "监测血压、意识、头晕或晕厥和皮肤颜色；避免与 PDE5 抑制剂或硝酸酯类药物合用。",
        "出现持续头痛、视力改变、明显乏力、呼吸困难或意识改变时立即就医。",
    ],
}

CATEGORY_MONITORING_EN = {
    "stimulant": [
        "Monitor heart rate, blood pressure, temperature and hydration; avoid alcohol, other stimulants and MAOIs.",
        "Seek urgent care for chest pain, hyperthermia, altered consciousness, seizures or persistent insomnia.",
    ],
    "depressant": [
        "Monitor consciousness, respiratory rate and temperature; avoid alcohol, opioids, benzodiazepines and other sedatives.",
        "Seek urgent help for shallow breathing, inability to wake the person, vomiting or seizures.",
    ],
    "opioid": [
        "Monitor breathing, sedation and pupil size; keep naloxone available and avoid using alone.",
        "Call emergency services for shallow breathing, blue lips or inability to wake the person.",
    ],
    "psychedelic": [
        "Monitor mental state, temperature and hydration; use in a safe, familiar setting with a trusted person present.",
        "Seek help for persistent panic, paranoia, psychotic symptoms or self-harm risk.",
    ],
    "dissociative": [
        "Monitor consciousness, blood pressure, temperature and injury risk; avoid using alone or with alcohol/sedatives.",
        "Watch for bladder and urinary symptoms and stop repeated use if they occur.",
    ],
    "cannabinoid": [
        "Monitor anxiety, palpitations, blood pressure and psychiatric symptoms; do not drive or operate machinery.",
        "People with psychosis risk or cardiovascular disease should avoid use.",
    ],
    "deliriant": [
        "Monitor consciousness, temperature, heart rate, urinary retention and seizures; avoid anticholinergics, alcohol and sedatives.",
        "Seek urgent care for hyperthermia, seizures, severe agitation or altered consciousness.",
    ],
    "entactogen": [
        "Monitor temperature, hydration and electrolytes, heart rate and blood pressure; avoid overheating, excessive or inadequate fluid intake, serotonergic medicines and MAOIs.",
        "Seek urgent care for hyperthermia, altered consciousness, muscle rigidity, seizures or severe headache.",
    ],
    "nootropic": [
        "Monitor sleep, mood, blood pressure and gastrointestinal effects; avoid stacking with prescription medicines.",
        "Long-term safety data are limited; stop and seek advice for persistent adverse effects.",
    ],
    "default": [
        "Monitor consciousness, breathing, heart rate, blood pressure and temperature; avoid mixing substances.",
        "Seek urgent care for severe or persistent symptoms.",
    ],
    "nitrite": [
        "Monitor blood pressure, consciousness, dizziness or syncope and skin colour; avoid PDE5 inhibitors and nitrate medicines.",
        "Seek urgent care for persistent headache, visual change, marked weakness, breathing difficulty or altered consciousness.",
    ],
}

RISK_KEYWORDS = (
    ("respiratory depression", "呼吸抑制"),
    ("overheat", "体温过高/中暑"),
    ("hyperthermia", "高热"),
    ("serotonin syndrome", "5-羟色胺综合征"),
    ("seizure", "癫痫发作"),
    ("psychosis", "精神病性症状"),
    ("paranoia", "偏执"),
    ("arrhythm", "心律失常"),
    ("cardiac", "心脏风险"),
    ("heart", "心脏风险"),
    ("liver", "肝损伤"),
    ("kidney", "肾损伤"),
    ("stroke", "卒中"),
    ("thrombosis", "血栓"),
    ("hyponatremia", "低钠血症"),
    ("bladder", "膀胱损伤"),
    ("withdrawal", "戒断反应"),
    ("dependence", "依赖"),
    ("addiction", "成瘾"),
    ("pregnancy", "妊娠风险"),
    ("asthma", "哮喘"),
    ("epilep", "癫痫"),
    ("dehydration", "脱水"),
    ("coma", "昏迷"),
    ("fatal", "可能致命"),
    ("death", "死亡风险"),
)

RISK_KEYWORDS_EN = (
    ("respiratory depression", "respiratory depression"),
    ("overheat", "overheating"),
    ("hyperthermia", "hyperthermia"),
    ("serotonin syndrome", "serotonin syndrome"),
    ("seizure", "seizures"),
    ("psychosis", "psychotic symptoms"),
    ("paranoia", "paranoia"),
    ("arrhythm", "arrhythmia"),
    ("cardiac", "cardiac risk"),
    ("heart", "cardiac risk"),
    ("liver", "liver injury"),
    ("kidney", "kidney injury"),
    ("stroke", "stroke"),
    ("thrombosis", "thrombosis"),
    ("hyponatremia", "hyponatremia"),
    ("bladder", "bladder injury"),
    ("withdrawal", "withdrawal"),
    ("dependence", "dependence"),
    ("addiction", "addiction"),
    ("pregnancy", "pregnancy risk"),
    ("asthma", "asthma"),
    ("epilep", "epilepsy"),
    ("dehydration", "dehydration"),
    ("coma", "coma"),
    ("fatal", "fatal risk"),
    ("death", "death risk"),
)

INTERACTION_ZH = {
    "depressant": "抑制剂",
    "depressants": "抑制剂",
    "dissociatives": "分离剂",
    "benzodiazepines": "苯二氮䓬类",
    "dextromethorphan": "右美沙芬",
    "opioids": "阿片类",
    "tramadol": "曲马多",
    "cocaine": "可卡因",
    "maoi": "MAOI",
    "pcp": "PCP",
    "hepatotoxic drugs": "肝毒性药物",
    "stimulants": "兴奋剂",
    "cannabis": "大麻",
    "substituted amphetamines": "取代安非他明类",
    "nitrous": "一氧化二氮",
    "ssris": "SSRIs",
    "snris": "SNRIs",
    "alcohol": "酒精",
    "ghb": "GHB",
    "gbl": "GBL",
    "ketamine": "氯胺酮",
    "methoxetamine": "甲氧西他明",
}


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


def unique(values):
    return list(dict.fromkeys(value for value in values if value))


def categories(entry):
    return set(entry.get("categories") or [])


def primary_category(entry):
    override = PRIMARY_CATEGORY_OVERRIDES.get(entry["name"])
    if override:
        return override
    present = categories(entry)
    for category_id in CATEGORY_PRIORITY:
        if category_id in present:
            return category_id
    return "default"


def category_label(entry):
    labels = [
        CATEGORY_ZH[category_id]
        for category_id in CATEGORY_PRIORITY
        if category_id in categories(entry)
    ]
    override = PRIMARY_CATEGORY_OVERRIDES.get(entry["name"])
    if override and override not in categories(entry):
        labels.append(CATEGORY_ZH[override])
    return "、".join(labels) if labels else "社区资料物质"


def category_label_en(entry):
    labels = [
        CATEGORY_EN[category_id]
        for category_id in CATEGORY_PRIORITY
        if category_id in categories(entry)
    ]
    override = PRIMARY_CATEGORY_OVERRIDES.get(entry["name"])
    if override and override not in categories(entry):
        labels.append(CATEGORY_EN[override])
    return ", ".join(labels) if labels else "community-documented"


def zh_name(entry):
    return NAME_ZH.get(entry["name"], entry["name"])


def clean_zh(text):
    if not text:
        return ""
    for source, target in (
        ("研究化学品", "新型精神活性物质"),
        ("研究性化学品", "新型精神活性物质"),
        ("研究物质", "新型精神活性物质"),
        ("学习索引", "资料索引"),
        ("研究用途", "资料用途"),
    ):
        text = text.replace(source, target)
    return re.sub(r"\s+", " ", text).strip()


def zh_summary(entry, page):
    if entry["name"] in SUMMARY_ZH_OVERRIDES:
        return SUMMARY_ZH_OVERRIDES[entry["name"]]
    translated = page.get("translations", {}).get("summary")
    if translated:
        return clean_zh(translated)
    if entry.get("summary"):
        return (
            f"{entry['name']} 在 PsychonautWiki 中被归为{category_label(entry)}类物质；"
            "现有英文摘要主要描述其主观效应，人体药代、剂量与长期安全性资料有限。"
        )
    return (
        f"{entry['name']} 在 PsychonautWiki 中被归为{category_label(entry)}类物质；"
        "现有社区资料主要提供身份与效应分类，人体药代、剂量和长期安全性证据有限。"
    )


def en_summary(entry, page):
    if entry.get("summary"):
        return entry["summary"].strip()
    extract = (page.get("extract") or "").strip()
    if extract:
        first = re.split(r"(?<=[.!?])\s+", extract)[0]
        return first[:600]
    return (
        f"{entry['name']} is documented as a {category_label_en(entry)} substance in community sources; "
        "clinical pharmacology, dose safety and long-term outcome data are limited."
    )


def translated_toxicity(value):
    return TOXICITY_ZH.get(value.strip().lower().rstrip("."))


def translated_addiction(value):
    return ADDICTION_ZH.get(value.strip().lower().rstrip("."))


def localized_duration(value):
    def replace(match):
        number = re.sub(r"\s*[-–]\s*", "–", match.group("range"))
        unit = DURATION_ZH[match.group("unit").lower()]
        return f"{number} {unit}"

    return DURATION_PATTERN.sub(replace, str(value).strip())


def translated_tolerance(entry):
    tolerance = entry.get("tolerance") or {}
    full = tolerance.get("full")
    half = tolerance.get("half")
    zero = tolerance.get("zero")
    if not (full or half or zero):
        return None
    parts = []
    if full:
        parts.append(TOLERANCE_FULL_ZH.get(full, full))
    if half:
        parts.append(f"半耐受恢复约 {localized_duration(half)}")
    if zero:
        parts.append(f"完全恢复约 {localized_duration(zero)}")
    return "；".join(parts)


def translated_cross_tolerances(entry):
    values = []
    for value in entry.get("crossTolerances") or []:
        for part in str(value).split("|"):
            translated = CROSS_ZH.get(part.strip().lower())
            if translated:
                values.append(translated)
    return unique(values)


def risk_keyword_lines(entry):
    text = " ".join(
        str(entry.get(key) or "")
        for key in ("generalRisks", "longtermRisks", "summary")
    ).lower()
    return unique(label for marker, label in RISK_KEYWORDS if marker in text)


def risk_keyword_lines_en(entry):
    text = " ".join(
        str(entry.get(key) or "")
        for key in ("generalRisks", "longtermRisks", "summary")
    ).lower()
    return unique(label for marker, label in RISK_KEYWORDS_EN if marker in text)


def source_for(entry, page):
    page_id = page.get("pageId")
    version = (page.get("revisionTimestamp") or str(page.get("revisionId") or ""))[:10]
    return {
        "id": f"pw-{page_id or page.get('revisionId')}",
        "url": page.get("url") or entry["url"],
        "title": f"PsychonautWiki: {entry['name']}",
        "region": "International",
        "version": version or "unknown",
        "accessedDate": TODAY,
        "sourceType": "community-documentation",
        "evidenceLevel": "COMMUNITY_DOCUMENTATION",
    }


def search_for(entry, page, note):
    return {
        "date": TODAY,
        "query": f"{entry['name']}: PsychonautWiki identity, summary and risk fields",
        "result": note,
        "sourceUrls": [page.get("url") or entry["url"]],
    }


def interaction_names(values):
    result = []
    for value in values or []:
        result.append(INTERACTION_ZH.get(str(value).strip().lower(), str(value)))
    return unique(result)


def major_interactions(entry):
    interactions = entry.get("interactions") or {}
    dangerous = interaction_names(interactions.get("dangerous"))
    unsafe = interaction_names(interactions.get("unsafe"))
    uncertain = interaction_names(interactions.get("uncertain"))
    if not (dangerous or unsafe or uncertain):
        return None
    parts = []
    if dangerous:
        parts.append("危险：" + "、".join(dangerous))
    if unsafe:
        parts.append("不安全：" + "、".join(unsafe))
    if uncertain:
        parts.append("不确定：" + "、".join(uncertain))
    return "社区资料列出的合用风险 — " + "；".join(parts) + "。"


def major_interactions_en(entry):
    interactions = entry.get("interactions") or {}
    dangerous = unique(interactions.get("dangerous") or [])
    unsafe = unique(interactions.get("unsafe") or [])
    uncertain = unique(interactions.get("uncertain") or [])
    if not (dangerous or unsafe or uncertain):
        return None
    parts = []
    if dangerous:
        parts.append("dangerous: " + ", ".join(dangerous))
    if unsafe:
        parts.append("unsafe: " + ", ".join(unsafe))
    if uncertain:
        parts.append("uncertain: " + ", ".join(uncertain))
    return "Community-listed interaction risks — " + "; ".join(parts) + "."


def major_warnings(entry, locale):
    category = primary_category(entry)
    if locale == "zh-CN":
        lines = [CATEGORY_WARNING_ZH.get(category, CATEGORY_WARNING_ZH["default"])]
        for value in entry.get("toxicities") or []:
            translated = translated_toxicity(value)
            if translated:
                lines.append(f"社区资料记录的毒性：{translated}。")
        addiction = translated_addiction(entry.get("addictionPotential") or "")
        if addiction:
            lines.append(f"依赖与滥用风险：{addiction}。")
        tolerance = translated_tolerance(entry)
        if tolerance:
            lines.append(f"耐受性：{tolerance}。")
        keywords = risk_keyword_lines(entry)
        if keywords:
            lines.append("社区资料特别提示：" + "、".join(keywords) + "。")
        lines.append(
            "以上为社区资料整理，不等同于药品说明书或临床警告；避免混合使用和独自使用，出现严重症状立即就医。"
        )
        return unique(lines)

    lines = [CATEGORY_WARNING_EN.get(category, CATEGORY_WARNING_EN["default"])]
    lines.extend(
        f"Community-reported toxicity: {value}."
        for value in entry.get("toxicities") or []
    )
    addiction = entry.get("addictionPotential")
    if addiction:
        lines.append(f"Dependence and abuse potential: {addiction}.")
    tolerance = entry.get("tolerance") or {}
    if any(tolerance.values()):
        tolerance_parts = []
        if tolerance.get("full"):
            tolerance_parts.append(f"full tolerance {tolerance['full']}")
        if tolerance.get("half"):
            tolerance_parts.append(f"half tolerance recovery about {tolerance['half']}")
        if tolerance.get("zero"):
            tolerance_parts.append(f"full recovery about {tolerance['zero']}")
        lines.append("Tolerance: " + "; ".join(tolerance_parts) + ".")
    keywords = risk_keyword_lines_en(entry)
    if keywords:
        lines.append(
            "Community sources specifically flag: " + ", ".join(keywords) + "."
        )
    lines.append(
        "This is community documentation, not a product label or clinical warning. Avoid mixing and using alone; seek emergency care for severe symptoms."
    )
    return unique(lines)


def monitoring(entry, locale):
    category = primary_category(entry)
    if locale == "zh-CN":
        return list(
            CATEGORY_MONITORING_ZH.get(category, CATEGORY_MONITORING_ZH["default"])
        )
    return list(CATEGORY_MONITORING_EN.get(category, CATEGORY_MONITORING_EN["default"]))


def review(status, source, note=None, reason=None):
    value = {"status": status, "reviewedDate": TODAY, "sourceIds": [source["id"]]}
    if note:
        value["note"] = note
    if reason:
        value["reason"] = reason
    return value


def build_decision(entry, page):
    source = source_for(entry, page)
    has_interactions = bool(entry.get("interactions"))
    has_summary = bool(
        entry.get("summary")
        or page.get("extract")
        or page.get("translations", {}).get("summary")
    )
    has_translation = bool(page.get("translations", {}).get("summary"))
    common_names = unique(
        [entry["name"], zh_name(entry), *(entry.get("commonNames") or [])]
    )
    fields = {
        "identity": review(
            "verified",
            source,
            note="PsychonautWiki 页面标题与稳定 ID 对应；未改变规范名称。",
        ),
        "commonNames": review(
            "verified",
            source,
            note="保留现有社区别名；中文名为常用译名（如有），不主张为监管批准名称。",
        ),
        "categories": review(
            "verified",
            source,
            note="保留 PW 效应类别；不将社区分类等同于临床诊断或适应证。",
        ),
        "roas": review(
            "verified",
            source,
            note="保留现有社区给药途径、剂量和时间字段；不解释为临床处方。",
        )
        if entry.get("roas")
        else {
            "status": "not_applicable",
            "reviewedDate": TODAY,
            "sourceIds": [source["id"]],
            "reason": "PW 页面未列给药途径；不推断缺失的给药途径。",
        },
        "summary": review(
            "verified",
            source,
            note="中文摘要来自 PW 页面翻译或既有摘要；关键数值以英文页面为准。",
        )
        if has_summary
        else {
            "status": "evidence_unavailable",
            "reviewedDate": TODAY,
            "sourceIds": [source["id"]],
            "reason": "PW 页面未提供可读摘要；中文概述仅依据现有分类与物质身份整理。",
        },
        "clinicalInfo.indications": {
            "status": "not_applicable",
            "reviewedDate": TODAY,
            "sourceIds": [source["id"]],
            "reason": "社区资料物质，未核验获批临床适应证；不提供适应证或用药建议。",
        },
        "clinicalInfo.contraindications": {
            "status": "evidence_unavailable",
            "reviewedDate": TODAY,
            "sourceIds": [source["id"]],
            "reason": "未找到经监管说明书核实的禁忌证；不以社区分类替代临床判断。",
        },
        "clinicalInfo.majorWarnings": review(
            "verified",
            source,
            note="据 PW 分类、毒性、依赖性与风险字段整理；不是药品说明书警告。",
        ),
        "clinicalInfo.majorInteractions": review(
            "verified",
            source,
            note="保留 PW 结构化相互作用分类；不替代临床判断。",
        )
        if has_interactions
        else {
            "status": "evidence_unavailable",
            "reviewedDate": TODAY,
            "sourceIds": [source["id"]],
            "reason": "PW 页面未提供结构化相互作用清单；不能据此排除未知相互作用。",
        },
        "clinicalInfo.monitoring": review(
            "verified",
            source,
            note="据 PW 风险与减少伤害资料整理；不是临床监测方案。",
        ),
        "doseUseReferences": {
            "status": "not_applicable",
            "reviewedDate": TODAY,
            "sourceIds": [source["id"]],
            "reason": "社区剂量阈值保留在现有 roas.dose 字段；未转换为临床剂量参考。",
        },
        "timeCourse": {
            "status": "not_applicable",
            "reviewedDate": TODAY,
            "sourceIds": [source["id"]],
            "reason": "社区起效/持续时间保留在现有 roas.duration 字段；未生成临床 timeCourse。",
        },
        "sourceRefs": review("verified", source),
        "translation": review(
            "verified",
            source,
            note="采用 PW 中文摘要；关键数值以英文页面为准。",
        )
        if has_translation
        else {
            "status": "evidence_unavailable",
            "reviewedDate": TODAY,
            "sourceIds": [source["id"]],
            "reason": "PW 页面无中文摘要；中文概述为编辑整理，未逐句翻译。",
        },
        "interactions": review(
            "verified",
            source,
            note="保留现有 PW 相互作用分类。",
        )
        if has_interactions
        else {
            "status": "not_applicable",
            "reviewedDate": TODAY,
            "sourceIds": [source["id"]],
            "reason": "PW 页面未提供结构化相互作用清单；不能据此排除未知相互作用。",
        },
    }
    assert set(fields) == set(FIELDS), entry["name"]

    zh_clinical = {
        "atcCodes": [],
        "drugClass": [],
        "indications": [
            "本条目为社区资料物质，未核验获批临床适应证；不提供适应证或用药建议。"
        ],
        "contraindications": [
            "未找到经监管说明书核实的禁忌证；如有心血管、精神、肝肾疾病、妊娠或正在服药，使用前应咨询专业人员。"
        ],
        "majorWarnings": major_warnings(entry, "zh-CN"),
        "majorInteractions": [
            major_interactions(entry)
            or "社区资料未提供系统相互作用清单；不能据此排除未知相互作用。避免与酒精、镇静剂、兴奋剂或其他精神活性物质混用。"
        ],
        "monitoring": monitoring(entry, "zh-CN"),
        "sourceRefs": [source],
    }
    en_clinical = {
        "atcCodes": [],
        "drugClass": [],
        "indications": [
            "Community-documented substance; no approved clinical indication was verified. No indication or prescribing advice is provided."
        ],
        "contraindications": [
            "No contraindication verified against a regulatory label. Cardiovascular, psychiatric, hepatic, renal, pregnancy and medication factors require professional review."
        ],
        "majorWarnings": major_warnings(entry, "en"),
        "majorInteractions": [
            major_interactions_en(entry)
            or "Community sources do not provide a structured interaction list; unknown interactions cannot be excluded. Avoid alcohol, sedatives, stimulants and other psychoactive substances."
        ],
        "monitoring": monitoring(entry, "en"),
        "sourceRefs": [source],
    }
    override = {
        "fields": {
            "sourceRefs": [source],
            "commonNames": common_names,
        },
        "locales": {
            "zh-CN": {
                "displayName": zh_name(entry),
                "summary": zh_summary(entry, page),
                "clinicalInfo": zh_clinical,
            },
            "en": {
                "displayName": entry["name"],
                "summary": en_summary(entry, page),
                "clinicalInfo": en_clinical,
            },
        },
    }
    return {
        "sources": [source],
        "searches": [
            search_for(
                entry,
                page,
                "已核对 PW 页面身份、摘要与风险字段，并保留本地现有 roas.dose/duration；未发现监管说明书。",
            )
        ],
        "aliasDecisions": [],
        "fields": fields,
        "override": override,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()
    manifest = read(DATA / "review_manifest.json")["entries"]
    baseline = read(DATA / "library_baseline.json")["entries"]
    substances = {
        item["id"]: item
        for item in read(ROOT / "app/src/main/res/raw/substances.json")["substances"]
    }
    pages = read(DEFAULT_OUTPUT / "psychonautwiki-baseline.json")
    baseline_ids = [item["id"] for item in baseline if item["group"] == "baseline"]
    batches = {}
    for identifier in baseline_ids:
        batches.setdefault(manifest[identifier]["batch"], []).append(identifier)
    written = 0
    for batch_name in sorted(batches):
        entries = {}
        for identifier in sorted(
            batches[batch_name], key=lambda item: substances[item]["name"]
        ):
            entry = substances[identifier]
            page = pages.get(entry["name"])
            assert page, f"{entry['name']}: missing PsychonautWiki page"
            decision = build_decision(entry, page)
            entries[identifier] = decision
        report = {
            "schemaVersion": 1,
            "batch": batch_name,
            "entries": entries,
        }
        write(args.output_dir / f"{batch_name}-curated.json", report)
        written += len(entries)
        print(f"{batch_name}: wrote {len(entries)} curated entries")
    assert written == len(baseline_ids), (written, len(baseline_ids))
    print(f"Wrote {written} baseline entries in {len(batches)} batches")


if __name__ == "__main__":
    main()
