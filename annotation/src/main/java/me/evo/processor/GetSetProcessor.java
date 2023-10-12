package me.evo.processor;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import me.evo.anno.GetterSetter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;
import java.util.function.Consumer;

@AutoService(Processor.class)
@SupportedAnnotationTypes("me.evo.anno.GetterSetter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GetSetProcessor extends AbstractProcessor {

    /**
     * 어노테이션 처리하는 구간
     * @param annotations the annotation types requested to be processed
     * @param roundEnv  environment for information about the current and prior round
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        //tool 프레임워크가 제공하는 처리 환경.
        TreeEdit treeModifier = new TreeEdit(processingEnv);


        treeModifier.setClassDefModifyStrategy(getAppendSetterAndGetterStrategy(treeModifier));

        // @GetterSetter을 붙인 모든 요소(클래스,인터페이스,enum,메서드)들을 가져온다
        // Gets all elements (classes, interfaces, enums, methods) that have @GetterSetter attached to them
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(GetterSetter.class);

        //해당 어노테이션이 붙인 요소들을 하나씩 확인
        for (Element element : elements) {

            // 요소의 간단한 이름을 반환 단지 출력해서 확인하는 용도
            // Returns the simple name of the element, just for printing and checking
            String elementName = element.getSimpleName().toString();

            //요소의 종류를 확인 만약 클래스가 아닌 경우 에러메세지 발생
            if (element.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation Error >> not supported type : " + elementName);
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing >>> " + elementName);
                //AST 순회 및 조작
                treeModifier.modifyTree(element);
            }


        }
        return true;
    }

    private Consumer<JCTree.JCClassDecl> getAppendSetterAndGetterStrategy(TreeEdit treeModifer) {
        TreeMaker treeMaker = treeModifer.getTreeMaker();
        Names names = treeModifer.getNames();
        //jcClassDecl 클래스 노드
        return jcClassDecl -> {
            //해당 클래스의 멤버들
            List<JCTree> members = jcClassDecl.getMembers();
            for (JCTree member : members) {
                //멤버 타입이 변수인 경우
                if (member instanceof JCTree.JCVariableDecl) {
                    // getter AST 생성
                    // Create a getter AST
                    JCTree.JCMethodDecl getter = createGetterTree(treeMaker, names, (JCTree.JCVariableDecl) member);

                    // setter AST 생성
                    // Create a setter AST
                    JCTree.JCMethodDecl setter = createSetterTree(treeMaker, names, (JCTree.JCVariableDecl) member);

                    //클래스 AST에 getter 추가
                    jcClassDecl.defs = jcClassDecl.defs.prepend(getter);

                    //클래스 AST에 setter 추가
                    jcClassDecl.defs = jcClassDecl.defs.prepend(setter);
                }
            }
        };
    }


    private JCTree.JCMethodDecl createSetterTree(TreeMaker treeMaker,
                                                 Names names,
                                                 JCTree.JCVariableDecl member) {

        //맨 앞글자 대문자로 변경 name=> Name , phoneNumber =>PhoneNumber
        String memberName = StringUtils.capitalize(member.name.toString());

        //set을 연결 setName, setPhoneNumber
        String setterName = "set".concat(memberName);

        //매개변수에 타입과 변수 이름을 조합. String Name int Phone_num
        JCTree.JCVariableDecl param = treeMaker.Param(names.fromString(memberName), member.vartype.type, null);

        return treeMaker.MethodDef(
                treeMaker.Modifiers(Flags.PUBLIC), //modifier 0: default, 1: public, 2: private
                names.fromString(setterName), //메서드 명
                treeMaker.TypeIdent(TypeTag.VOID), //return type
                List.nil(),
                List.of(param), //parameter
                List.nil(),
                treeMaker.Block(0,
                        // 변수와 매개변수를 exec을 통해서 할당 this.name = name과 같은 맥락
                        List.of(treeMaker.Exec(treeMaker.Assign(treeMaker.Ident(member), treeMaker.Ident(param.name)))) //body
                ),
                null
        );
    }

    private JCTree.JCMethodDecl createGetterTree(TreeMaker treeMaker,
                                                 Names names,
                                                 JCTree.JCVariableDecl member) {
        String memberName = StringUtils.capitalize(member.name.toString());
        String getterName = "get".concat(memberName);

        return treeMaker.MethodDef(
                treeMaker.Modifiers(1),
                names.fromString(getterName),
                (JCTree.JCExpression) member.getType(),
                List.nil(),
                List.nil(),
                List.nil(),
                treeMaker.Block(1,
                        List.of(treeMaker.Return(treeMaker.Ident(member.getName())))
                ),
                null
        );
    }

}
