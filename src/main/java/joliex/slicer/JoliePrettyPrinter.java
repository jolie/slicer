/*
 * Copyright (C) 2021 Valentino Picotti
 * Copyright (C) 2021 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package joliex.slicer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jolie.lang.Constants;
import jolie.lang.parse.UnitOLVisitor;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.DivideAssignStatement;
import jolie.lang.parse.ast.DocumentationComment;
import jolie.lang.parse.ast.EmbedServiceNode;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ForEachArrayItemStatement;
import jolie.lang.parse.ast.ForEachSubNodeStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.ImportStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.InterfaceExtenderDefinition;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.MultiplyAssignStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.ProvideUntilStatement;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SubtractAssignStatement;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.InterfaceOneWayBranch;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.InterfaceRequestResponseBranch;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.OperationOneWayBranch;
import jolie.lang.parse.ast.courier.CourierChoiceStatement.OperationRequestResponseBranch;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.AndConditionNode;
import jolie.lang.parse.ast.expression.ConstantBoolExpression;
import jolie.lang.parse.ast.expression.ConstantDoubleExpression;
import jolie.lang.parse.ast.expression.ConstantIntegerExpression;
import jolie.lang.parse.ast.expression.ConstantLongExpression;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.expression.FreshValueExpressionNode;
import jolie.lang.parse.ast.expression.IfExpressionNode;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.InstanceOfExpressionNode;
import jolie.lang.parse.ast.expression.IsTypeExpressionNode;
import jolie.lang.parse.ast.expression.NotExpressionNode;
import jolie.lang.parse.ast.expression.OrConditionNode;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.expression.SolicitResponseExpressionNode;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.lang.parse.ast.expression.VoidExpressionNode;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode.AssignmentOperation;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode.DeepCopyOperation;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode.PointsToOperation;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.util.Pair;
import jolie.util.Range;


public class JoliePrettyPrinter implements UnitOLVisitor {
	final PrettyPrinter pp = new PrettyPrinter();
	boolean isTopLevelTypeDeclaration = true;
	boolean printOnlyLinkedTypeName = false;

	public String toString() {
		return pp.pp.toString();
	}

	@Override
	public void visit( Program n ) {
		pp.intercalate( n.children(),
			( child, _0 ) -> child.accept( this ),
			PrettyPrinter::newline );
	}

	@Override
	public void visit( OneWayOperationDeclaration decl ) {
		pp.append( decl.id() )
			.spacedParens( asPPConsumer( PrettyPrinter::append, decl.requestType().name() ) );
	}

	@Override
	public void visit( RequestResponseOperationDeclaration decl ) {
		// TODO: Print faults
		pp.append( decl.id() )
			.spacedParens( asPPConsumer( PrettyPrinter::append, decl.requestType().name() ) )
			.spacedParens( asPPConsumer( PrettyPrinter::append, decl.responseType().name() ) );
	}

	@Override
	public void visit( DefinitionNode n ) {
		pp.append( n.id() )
			.space()
			.newCodeBlock( _0 -> n.body().accept( this ) );
	}

	@Override
	public void visit( ParallelStatement n ) {
		pp.append( "PARALLEL STMT:" )
			.newline()
			.intercalate( n.children(),
				( child, pp ) -> pp.newCodeBlock( _0 -> child.accept( this ) ),
				pp -> pp.space().append( '|' ).newline() );
	}

	@Override
	public void visit( NDChoiceStatement n ) {
		pp.intercalate( n.children(),
			( pair, pp ) -> pp // Pair (input-guard, process)
				.spacedBrackets( _0 -> pair.key().accept( this ) )
				.newCodeBlock( _0 -> pair.value().accept( this ) ),
			PrettyPrinter::newline );
	}

	@Override
	public void visit( SequenceStatement n ) {
		pp.intercalate( n.children(),
			( child, _0 ) -> child.accept( this ),
			PrettyPrinter::newline );
	}

	@Override
	public void visit( OneWayOperationStatement n ) {
		pp.append( n.id() )
			.spacedParens( _0 -> n.inputVarPath().accept( this ) );
	}

	@Override
	public void visit( RequestResponseOperationStatement n ) {
		pp.append( n.id() )
			.spacedParens( _0 -> n.inputVarPath().accept( this ) )
			.spacedParens( _0 -> n.outputExpression().accept( this ) )
			.newCodeBlock( _0 -> n.process().accept( this ) );
	}

	@Override
	public void visit( NotificationOperationStatement n ) {
		pp.append( n.id() )
			.append( '@' )
			.append( n.outputPortId() )
			.spacedParens( _0 -> n.outputExpression().accept( this ) );
	}

	@Override
	public void visit( SolicitResponseOperationStatement n ) {
		// TODO pretty print install function node at the end
		pp.append( n.id() )
			.append( '@' )
			.append( n.outputPortId() )
			.spacedParens( _0 -> _0
				.ifPresent( Optional.ofNullable( n.outputExpression() ),
					( outExpre, _1 ) -> outExpre.accept( this ) ) )
			.spacedParens( _0 -> _0
				.ifPresent(
					Optional.ofNullable( n.inputVarPath() ),
					( varPath, _1 ) -> varPath.accept( this ) ) );
	}

	@Override
	public void visit( LinkInStatement n ) {
		assert false;
	}

	@Override
	public void visit( LinkOutStatement n ) {
		assert false;
	}

	@Override
	public void visit( AssignStatement n ) {
		n.variablePath().accept( this );
		pp.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, '=' ) );
		n.expression().accept( this );
	}

	@Override
	public void visit( AddAssignStatement n ) {
		n.variablePath().accept( this );
		pp.append( "+=" );
		n.expression().accept( this );
	}

	@Override
	public void visit( SubtractAssignStatement n ) {
		n.variablePath().accept( this );
		pp.append( "-=" );
		n.expression().accept( this );
	}

	@Override
	public void visit( MultiplyAssignStatement n ) {
		n.variablePath().accept( this );
		pp.append( "*=" );
		n.expression().accept( this );
	}

	@Override
	public void visit( DivideAssignStatement n ) {
		n.variablePath().accept( this );
		pp.append( "%=" );
		n.expression().accept( this );
	}

	@Override
	public void visit( IfStatement n ) {
		pp
			.intercalate( n.children(),
				( pair, _0 ) -> _0 // Pair (condition, body)
					.append( "if" )
					.spacedParens( _1 -> pair.key().accept( this ) )
					.newCodeBlock( _1 -> pair.value().accept( this ) ),
				_0 -> _0
					.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "else" ) ) )
			.ifPresent(
				Optional.ofNullable( n.elseProcess() ),
				( proc, _0 ) -> _0
					.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "else" ) )
					.newCodeBlock( _1 -> proc.accept( this ) ) );
	}

	@Override
	public void visit( DefinitionCallStatement n ) {
		pp.append( n.id() );
	}

	@Override
	public void visit( WhileStatement n ) {
		pp.append( "while" )
			.spacedParens( _0 -> n.condition().accept( this ) )
			.newCodeBlock( _0 -> n.body().accept( this ) );
	}

	@Override
	public void visit( OrConditionNode n ) {
		pp.intercalate( n.children(),
			( andCond, _0 ) -> andCond.accept( this ),
			_0 -> _0
				.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "||" ) ) );
	}

	@Override
	public void visit( AndConditionNode n ) {
		pp.intercalate( n.children(),
			( andCond, _0 ) -> andCond.accept( this ),
			_0 -> _0
				.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "&&" ) ) );
	}

	@Override
	public void visit( NotExpressionNode n ) {
		pp.append( '!' );
		n.expression().accept( this );
	}

	@Override
	public void visit( CompareConditionNode n ) {
		n.leftExpression().accept( this );
		String op = " ";
		switch( n.opType() ) {
		case EQUAL:
			op = "==";
			break;
		case LANGLE:
			op = "<";
			break;
		case RANGLE:
			op = ">";
			break;
		case MAJOR_OR_EQUAL:
			op = ">=";
			break;
		case MINOR_OR_EQUAL:
			op = "<=";
			break;
		case NOT_EQUAL:
			op = "!=";
			break;
		default:
			assert (false);
		}
		pp.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, op ) );
		n.rightExpression().accept( this );
	}

	@Override
	public void visit( ConstantIntegerExpression n ) {
		pp.append( n.value() );
	}

	@Override
	public void visit( ConstantDoubleExpression n ) {
		pp.append( n.value() );
	}

	@Override
	public void visit( ConstantBoolExpression n ) {
		pp.append( String.valueOf( n.value() ) );
	}

	@Override
	public void visit( ConstantLongExpression n ) {
		pp.append( n.value() ).append( "L" );
	}

	@Override
	public void visit( ConstantStringExpression n ) {
		pp.surround( '"', asPPConsumer( PrettyPrinter::append, n.value() ) );
	}

	@Override
	public void visit( ProductExpressionNode n ) {
		Iterator< Pair< Constants.OperandType, OLSyntaxNode > > it = n.operands().iterator();
		if( it.hasNext() ) {
			it.next().value().accept( this );
		}
		it.forEachRemaining( ( operand ) -> {
			pp.space();
			switch( operand.key() ) {
			case MULTIPLY:
				pp.append( '*' );
				break;
			case DIVIDE:
				pp.append( '/' );
				break;
			case MODULUS:
				pp.append( '%' );
				break;
			default:
			}
			pp.space();
			operand.value().accept( this );
		} );
	}

	@Override
	public void visit( SumExpressionNode n ) {
		Iterator< Pair< Constants.OperandType, OLSyntaxNode > > it = n.operands().iterator();
		if( it.hasNext() ) {
			it.next().value().accept( this );
		}
		it.forEachRemaining( ( operand ) -> {
			pp.space();
			switch( operand.key() ) {
			case ADD:
				pp.append( '+' );
				break;
			case SUBTRACT:
				pp.append( '-' );
				break;
			default:
			}
			pp.space();
			operand.value().accept( this );
		} );
	}

	@Override
	public void visit( VariableExpressionNode n ) {
		n.variablePath().accept( this );
	}

	@Override
	public void visit( NullProcessStatement n ) {
		pp.append( "nullProcess" );
	}

	@Override
	public void visit( Scope n ) {
		pp.append( "scope" )
			.space()
			.spacedParens( _0 -> n.id() )
			.newCodeBlock( _0 -> n.body().accept( this ) );
	}

	@Override
	public void visit( InstallStatement n ) {
		pp.append( "install" )
			.spacedParens( _0 -> _0
				.intercalate( Arrays.asList( n.handlersFunction().pairs() ),
					( pair, _1 ) -> _1
						.append( pair.key() == null ? "this" : pair.key() )
						.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "=>" ) )
						// TODO: Don't know if braces of code block are fine here
						.newCodeBlock( _2 -> pair.value().accept( this ) ),
					_1 -> _1.comma().newline() ) );
	}

	@Override
	public void visit( CompensateStatement n ) {
		pp.append( "comp" )
			.spacedParens( asPPConsumer( PrettyPrinter::append, n.id() ) );
	}

	@Override
	public void visit( ThrowStatement n ) {
		pp.append( "throw" )
			.spacedParens( _0 -> _0
				.append( n.id() )
				.ifPresent(
					Optional.ofNullable( n.expression() ),
					( expr, pp ) -> {
						pp.comma().space();
						expr.accept( this );
					} ) );
	}

	@Override
	public void visit( ExitStatement n ) {
		pp.append( "exit" );
	}

	@Override
	public void visit( ExecutionInfo n ) {
		pp.append( "execution" )
			.colon()
			.space()
			.append( n.mode().name().toLowerCase() );
	}

	@Override
	public void visit( CorrelationSetInfo n ) {
		pp.append( "cset" )
			.space()
			.newCodeBlock( _0 -> _0
				.intercalate( n.variables(),
					( correlationVariableInfo, _1 ) -> {
						correlationVariableInfo.correlationVariablePath().accept( this );
						_1.surround( PrettyPrinter::space, PrettyPrinter::colon )
							.intercalate( correlationVariableInfo.aliases(),
								( aliasInfo, _2 ) -> {
									boolean backup = isTopLevelTypeDeclaration;
									isTopLevelTypeDeclaration = false;
									aliasInfo.guardName().accept( this );
									isTopLevelTypeDeclaration = backup;
									_2.dot();
									aliasInfo.variablePath().accept( this );
								},
								PrettyPrinter::space );
					},
					_1 -> _1.comma().newline() ) );
	}

	@Override
	public void visit( InputPortInfo n ) {
		pp.append( "inputPort" )
			.space()
			.append( n.id() )
			.space()
			.newCodeBlock( _0 -> _0
				.append( "location" )
				.colon()
				.space()
				.run( _1 -> n.location().accept( this ) )
				.newline()
				.onlyIf( n.protocol() != null, _1 -> _1
					.append( "protocol" )
					.colon()
					.space()
					.run( _2 -> n.protocol().accept( this ) )
					.newline() )
				.onlyIf( !n.getInterfaceList().isEmpty(), _1 -> _1
					.append( "interfaces" )
					.colon()
					.nest( _2 -> _2
						.ifTrueOrElse( n.getInterfaceList().size() > 1,
							PrettyPrinter::newline,
							PrettyPrinter::space )
						.intercalate( n.getInterfaceList(),
							( id, pp ) -> pp.append( id.name() ),
							pp -> pp.comma().newline() ) ) )
				.onlyIf( n.aggregationList().length > 0, _1 -> _1
					// TODO: pretty print aggregates
					.append( "aggregates" )
					.colon()
					.space()
					.append( "NOT IMPLEMENTED" ) )
				.onlyIf( !n.redirectionMap().isEmpty(), _1 -> _1
					// TODO: pretty print redirects
					.append( "redirects" )
					.colon()
					.space()
					.append( "NOT IMPLEMENTED" ) ) );
	}

	@Override
	public void visit( OutputPortInfo n ) {
		pp.append( "outputPort" )
			.space()
			.append( n.id() )
			.space()
			.newCodeBlock( _0 -> _0
				.onlyIf( n.location() != null, _1 -> _1
					.append( "location" )
					.colon()
					.space()
					.run( _2 -> n.location().accept( this ) )
					.newline() )
				.onlyIf( n.protocol() != null, _1 -> _1
					.append( "protocol" )
					.colon()
					.space()
					.run( _2 -> n.protocol().accept( this ) )
					.newline() )
				.onlyIf( !n.getInterfaceList().isEmpty(), _1 -> _1
					.append( "interfaces" )
					.colon()
					.nest( _2 -> _2
						.ifTrueOrElse( n.getInterfaceList().size() > 1,
							PrettyPrinter::newline,
							PrettyPrinter::space )
						.intercalate( n.getInterfaceList(),
							( id, pp ) -> pp.append( id.name() ),
							pp -> pp.comma().newline() ) ) ) );
	}

	@Override
	public void visit( PointerStatement n ) {
		n.leftPath().accept( this );
		pp.append( "->" );
		n.rightPath().accept( this );
	}

	@Override
	public void visit( DeepCopyStatement n ) {
		n.leftPath().accept( this );
		pp.surround(
			PrettyPrinter::space,
			asPPConsumer( PrettyPrinter::append, n.copyLinks() ? "<<-" : "<<" ) ) ;
		n.rightExpression().accept( this );
	}

	@Override
	public void visit( RunStatement n ) {
		assert false;
	}

	@Override
	public void visit( UndefStatement n ) {
		pp.append( "undef" )
			.spacedParens( _0 -> n.variablePath().accept( this ) );
	}

	@Override
	public void visit( ValueVectorSizeExpressionNode n ) {
		pp.append( "#" );
		n.variablePath().accept( this );
	}

	@Override
	public void visit( PreIncrementStatement n ) {
		pp.append( "++" );
		n.variablePath().accept( this );
	}

	@Override
	public void visit( PostIncrementStatement n ) {
		n.variablePath().accept( this );
		pp.append( "++" );
	}

	@Override
	public void visit( PreDecrementStatement n ) {
		pp.append( "--" );
		n.variablePath().accept( this );
	}

	@Override
	public void visit( PostDecrementStatement n ) {
		n.variablePath().accept( this );
		pp.append( "--" );
	}

	@Override
	public void visit( ForStatement n ) {
		pp.append( "for" )
			.spacedParens( pp -> {
				n.init().accept( this );
				pp.comma().space();
				n.condition().accept( this );
				pp.comma().space();
				n.post().accept( this );
			} )
			.newCodeBlock( _0 -> n.body().accept( this ) );
	}

	@Override
	public void visit( ForEachSubNodeStatement n ) {
		pp.append( "foreach" )
			.spacedParens( _0 -> {
				n.keyPath().accept( this );
				_0.surround( PrettyPrinter::space, PrettyPrinter::colon );
				n.targetPath().accept( this ) ;
			 } )
			.newCodeBlock( _0 -> n.body().accept( this ) );
	}

	@Override
	public void visit( ForEachArrayItemStatement n ) {
		pp.append( "for" )
			.spacedParens( _0 -> {
				n.keyPath().accept( this );
				_0.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "in" ) );
				n.targetPath().accept( this );
			} )
			.newCodeBlock( _0 -> n.body().accept( this ) );
	}

	@Override
	public void visit( SpawnStatement n ) {
		pp.append( "spawn" )
			.spacedParens( pp -> {
				n.indexVariablePath().accept( this );
				pp.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "over" ) );
				n.upperBoundExpression().accept( this );
			} )
			.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "in" ) )
			.run( _0 -> n.inVariablePath().accept( this ) )
			.space()
			.newCodeBlock( _0 -> n.body().accept( this ) );
	}

	@Override
	public void visit( IsTypeExpressionNode n ) {
		pp.append( "is_" + n.type().name().toLowerCase() )
			.spacedParens( _0 -> n.variablePath().accept( this ) );
	}

	@Override
	public void visit( InstanceOfExpressionNode n ) {
		n.expression().accept( this );
		pp.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "instanceof" ) );
		boolean backup = isTopLevelTypeDeclaration;
		isTopLevelTypeDeclaration = false;
		n.type().accept( this );
		isTopLevelTypeDeclaration = backup;
	}

	@Override
	public void visit( TypeCastExpressionNode n ) {
		pp.append( n.type().name().toLowerCase() )
			.spacedParens( _0 -> n.expression().accept( this ) );
	}

	@Override
	public void visit( SynchronizedStatement n ) {
		pp.append( "synchronized" )
			.spacedParens( asPPConsumer( PrettyPrinter::append, n.id() ) )
			.newCodeBlock( _0 -> n.body().accept( this ) );
	}

	@Override
	public void visit( CurrentHandlerStatement n ) {
		pp.append( "cH" );
	}

	@Override
	public void visit( EmbeddedServiceNode n ) {
		switch( n.type() ) {
		case INTERNAL:
			n.program().accept( this );
			break;
		case JOLIE:
		case JAVA:
		case JAVASCRIPT:
			pp.append( "embedded" )
				.space()
				.newCodeBlock( _0 -> _0
					.append( n.type().toString() )
					.surround( PrettyPrinter::space, PrettyPrinter::colon )
					.surround( '"', asPPConsumer( PrettyPrinter::append, n.servicePath() ) )
					.ifPresent( Optional.ofNullable( n.portId() ),
						( id, _1 ) -> _1
							.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "in" ) )
							.append( n.portId() ) ) );
			break;
		default:
			assert false;
		}
	}

	@Override
	public void visit( InstallFixedVariableExpressionNode n ) {
		pp.append( '^' );
		n.variablePath().accept( this );
	}

	@Override
	public void visit( VariablePathNode n ) {
		pp.onlyIf( n.isGlobal(), _0 -> _0.append( "global" ).dot() )
			.intercalate( n.path(),
				(element, pp) -> {
					OLSyntaxNode node = element.key();
					pp
					.ifTrueOrElse(
						node instanceof ConstantStringExpression,
						pp1 -> pp1.append( ( (ConstantStringExpression) node ).value() ),
						pp1 -> pp1.spacedParens( _0 -> node.accept( this ) ) )
					.ifPresent(
						Optional.ofNullable( element.value() ),
						(value, pp1) -> pp1.brackets( _0 -> value.accept( this ) ) );
				},
				PrettyPrinter::dot );
	}

	@Override
	public void visit( TypeInlineDefinition n ) {
		pp.onlyIf( isTopLevelTypeDeclaration, pp -> pp.append( "type" ).space() )
			.onlyIf( !printOnlyLinkedTypeName, pp -> pp
				.append( n.name() )
				.run( _0 -> printTypeCardinality( n.cardinality() ) )
				.colon()
				.space() )
			.append( n.basicType().nativeType().id() )
			.onlyIf( n.subTypes() != null && !n.subTypes().isEmpty(), _0 -> _0
				.space()
				.newCodeBlock( pp -> {
					boolean previousValue = isTopLevelTypeDeclaration;
					isTopLevelTypeDeclaration = false;
					List< Map.Entry< String, TypeDefinition > > subTypes = new ArrayList<>( n.subTypes() );
					subTypes.sort( Comparator.<Map.Entry< String, TypeDefinition >,Integer>comparing( entry -> entry.getValue().context().startLine()) );
					pp.intercalate( subTypes,
						( entry, _1 ) -> entry.getValue().accept( this ),
						PrettyPrinter::newline );
					isTopLevelTypeDeclaration = previousValue;
				} ) );
	}

	@Override
	public void visit( TypeDefinitionLink n ) {
		pp.onlyIf( isTopLevelTypeDeclaration, pp -> pp.append( "type" ).space() )
		    .onlyIf( !printOnlyLinkedTypeName, pp -> pp
				.append( n.name() )
				.run( _0 -> printTypeCardinality( n.cardinality() ) )
				.colon()
				.space() )
			.append( n.linkedTypeName() );
	}

	private void printTypeCardinality( Range r ) {
		if( Constants.RANGE_ONE_TO_ONE.equals(r) )
			return;

		if( r.max() == 1 ) { // min cannot be 1 here, so it must be 0
			pp.append('?');
		} else if ( r.min() == 0 && r.max() == Integer.MAX_VALUE ) {
			pp.append('*');
		} else {
			pp.brackets( _0 -> _0
				.append( r.min() )
				.comma()
				.ifTrueOrElse( r.max() == Integer.MAX_VALUE, 
					asPPConsumer( PrettyPrinter::append, '*'),
					asPPConsumer( PrettyPrinter::append, r.max() ) ) );
		}
	}

	@Override
	public void visit( InterfaceDefinition n ) {
		pp.append( "interface" )
			.space()
			.append( n.name() )
			.space()
			.newCodeBlock( pp -> {
				Stream< OperationDeclaration > s = n.operationsMap().values().stream();
				Map< Boolean, List< OperationDeclaration > > operations =
					s.collect( Collectors.partitioningBy( op -> op instanceof OneWayOperationDeclaration ) );
				List< OperationDeclaration > oneWayOperations = operations.get( true );
				List< OperationDeclaration > requestResponseOperations = operations.get( false );
				pp
					.onlyIf( !oneWayOperations.isEmpty(), _0 -> _0
						.append( "OneWay" )
						.colon()
						.nest( _1 -> _1
							.newline()
							.intercalate( oneWayOperations,
								( opDecl, _2 ) -> opDecl.accept( this ),
								_2 -> _2.comma().newline() ) )
						.onlyIf( !requestResponseOperations.isEmpty(), PrettyPrinter::newline ) )
					.onlyIf( !requestResponseOperations.isEmpty(), _0 -> _0
						.append( "RequestResponse" )
						.colon()
						.nest( _1 -> _1
							.newline()
							.intercalate( requestResponseOperations,
								( opDecl, _2 ) -> opDecl.accept( this ),
								_2 -> _2.comma().newline() ) ) );
			} );
	}

	@Override
	public void visit( DocumentationComment n ) {
		assert false;
	}

	@Override
	public void visit( FreshValueExpressionNode n ) {
		pp.append( "new" );
	}

	@Override
	public void visit( CourierDefinitionNode n ) {
		pp.append( "courier" )
			.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, n.inputPortName() ) )
			.newCodeBlock( _0 -> n.body().accept( this ) );
	}

	@Override
	public void visit( CourierChoiceStatement n ) {

		Stream< Object > choices =
			Stream.of( n.interfaceOneWayBranches(),
				n.interfaceRequestResponseBranches(),
				n.operationOneWayBranches(),
				n.operationRequestResponseBranches() )
				.flatMap( Collection::stream );

		pp.intercalate( choices.iterator(),
			( obj, pp ) -> {
				if( obj instanceof InterfaceOneWayBranch ) {
					InterfaceOneWayBranch iface = (InterfaceOneWayBranch) obj;
					pp.append( iface.interfaceDefinition.name() )
						.spacedParens( _1 -> iface.inputVariablePath.accept( this ) )
						.newCodeBlock( _1 -> iface.body.accept( this ) );
				} else if( obj instanceof InterfaceRequestResponseBranch ) {
					InterfaceRequestResponseBranch iface = (InterfaceRequestResponseBranch) obj;
					pp.append( iface.interfaceDefinition.name() )
						.spacedParens( _1 -> iface.inputVariablePath.accept( this ) )
						.spacedParens( _1 -> iface.outputVariablePath.accept( this ) )
						.newCodeBlock( _1 -> iface.body.accept( this ) );
				} else if( obj instanceof OperationOneWayBranch ) {
					OperationOneWayBranch op = (OperationOneWayBranch) obj;
					pp.append( op.operation )
						.spacedParens( _1 -> op.inputVariablePath.accept( this ) )
						.newCodeBlock( _1 -> op.body.accept( this ) );
				} else if( obj instanceof OperationRequestResponseBranch ) {
					OperationRequestResponseBranch op = (OperationRequestResponseBranch) obj;
					pp.append( op.operation )
						.spacedParens( _1 -> op.inputVariablePath.accept( this ) )
						.spacedParens( _1 -> op.outputVariablePath.accept( this ) )
						.newCodeBlock( _1 -> op.body.accept( this ) );
				}
			},
			PrettyPrinter::newline );
	}

	@Override
	public void visit( NotificationForwardStatement n ) {
		// TODO Auto-generated method stub
		assert false : "not implemented";
	}

	@Override
	public void visit( SolicitResponseForwardStatement n ) {
		// TODO Auto-generated method stub
		assert false : "not implemented";
	}

	@Override
	public void visit( InterfaceExtenderDefinition n ) {
		// TODO Auto-generated method stub
		assert false : "not implemented";
	}

	@Override
	public void visit( InlineTreeExpressionNode n ) {
		n.rootExpression().accept( this );
		pp.newCodeBlock( _0 -> _0
			.intercalate( 
				Arrays.asList( n.operations() ), 
				( operation, pp ) -> {
					if( operation instanceof AssignmentOperation ) {
						AssignmentOperation op = (AssignmentOperation) operation;
						op.path().accept( this );
						pp.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "=" ) );
						op.expression().accept( this ); 
					} else if ( operation instanceof DeepCopyOperation ) {
						DeepCopyOperation op = (DeepCopyOperation) operation;
						op.path().accept( this );
						pp.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "<<" ) );
						op.expression().accept( this ); 
					} else if ( operation instanceof PointsToOperation ) {
						PointsToOperation op = (PointsToOperation) operation;
						op.path().accept( this );
						pp.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "->" ) );
						op.target().accept( this ); 
					}
				},
				PrettyPrinter::newline ) );
	}

	@Override
	public void visit( VoidExpressionNode n ) {
	}

	@Override
	public void visit( ProvideUntilStatement n ) {
		pp.append( "provide" )
			.nest( pp -> {
				pp.newline();
				n.provide().accept( this );
			} )
			.newline()
			.append( "until" )
			.nest( pp -> {
				pp.newline();
				n.until().accept( this );
			} ) ;
	}

	@Override
	public void visit( TypeChoiceDefinition n ) {
		n.left().accept( this );
		pp.space().append( '|' ).space();
		boolean _isTopLevelTypeDeclaration = isTopLevelTypeDeclaration;
		boolean _printOnlyLinkedTypeName = printOnlyLinkedTypeName;
		isTopLevelTypeDeclaration = false;
		printOnlyLinkedTypeName = true;
		n.right().accept( this );
		isTopLevelTypeDeclaration = _isTopLevelTypeDeclaration;
		printOnlyLinkedTypeName = _printOnlyLinkedTypeName;
	}

	@Override
	public void visit( ImportStatement n ) {
		pp.append( "from" )
			.surround(
				PrettyPrinter::space,
				asPPConsumer(PrettyPrinter::append, n.prettyPrintTarget() ) )
			.append( "import" )
			.space()
			.intercalate(
				Arrays.asList( n.importSymbolTargets() ),
				( symbol, pp ) -> pp.append( symbol.toString() ),
				PrettyPrinter::comma );
	}

	@Override
	public void visit( ServiceNode n ) {
		pp.append( "service" )
			.space()
			.append( n.name() )
			.space()
			.ifPresent( n.parameterConfiguration(), ( param, _0 ) -> _0
				.spacedParens( _1 -> _1
					.append( param.variablePath() )
					.space()
					.colon()
					.space()
					.append( param.type().name() ) ) )
			.newCodeBlock( _0 -> n.program().accept( this ) );
	}

	@Override
	public void visit( EmbedServiceNode n ) {
		pp.append( "embed" )
			.space()
			.append( n.serviceName() )
			.onlyIf( n.passingParameter() != null, _0 -> _0
				.parens( _1 -> n.passingParameter().accept( this ) ) )
			.onlyIf( n.bindingPort() != null, _0 -> _0
				.space()
				.append( n.isNewPort() ? "as" : "in" )
				.space()
				.append( n.bindingPort().id() ) );
	}

	public static < T > Consumer< PrettyPrinter > asPPConsumer( BiConsumer< PrettyPrinter, T > prettyPrinterBiConsumer,
		T arg ) {
		return pp -> prettyPrinterBiConsumer.accept( pp, arg );
	}

	private static class PrettyPrinter {
		StringBuilder pp = new StringBuilder( 1000 );
		int indentationLevel = 0;

		public PrettyPrinter append( String a ) {
			pp.append( a );
			return this;
		}


		public PrettyPrinter append( char a ) {
			pp.append( a );
			return this;
		}

		public PrettyPrinter append( int a ) {
			pp.append( a );
			return this;
		}

		public PrettyPrinter append( long a ) {
			pp.append( a );
			return this;
		}

		public PrettyPrinter append( float a ) {
			pp.append( a );
			return this;
		}

		public PrettyPrinter append( double a ) {
			pp.append( a );
			return this;
		}

		public PrettyPrinter newline() {
			pp.append( System.lineSeparator() );
			for( int i = 0; i < indentationLevel; ++i ) {
				pp.append( '\t' );
			}
			return this;
		}

		public static < T > Consumer< PrettyPrinter > toConsumer(
			BiConsumer< PrettyPrinter, T > prettyPrinterBiConsumer, T arg ) {
			return pp -> prettyPrinterBiConsumer.accept( pp, arg );
		}

		private PrettyPrinter run( Consumer< PrettyPrinter > prettyPrinter ) {
			prettyPrinter.accept( this );
			return this;
		}

		public PrettyPrinter surround( String ldelimiter, String rdelimiter, Consumer< PrettyPrinter > prettyPrinter ) {
			return append( ldelimiter )
				.run( prettyPrinter )
				.append( rdelimiter );
		}

		public PrettyPrinter surround( char ldelimiter, char rdelimiter, Consumer< PrettyPrinter > prettyPrinter ) {
			return append( ldelimiter )
				.run( prettyPrinter )
				.append( rdelimiter );
		}

		public PrettyPrinter surround( Consumer< PrettyPrinter > ldelimiter, Consumer< PrettyPrinter > rdelimiter,
			Consumer< PrettyPrinter > prettyPrinter ) {
			return run( ldelimiter.andThen( prettyPrinter ).andThen( rdelimiter ) );
		}

		public PrettyPrinter surround( String delimiter, Consumer< PrettyPrinter > prettyPrinter ) {
			return surround( delimiter, delimiter, prettyPrinter );
		}

		public PrettyPrinter surround( char delimiter, Consumer< PrettyPrinter > prettyPrinter ) {
			return surround( delimiter, delimiter, prettyPrinter );
		}

		public PrettyPrinter surround( Consumer< PrettyPrinter > delimiter, Consumer< PrettyPrinter > prettyPrinter ) {
			return surround( delimiter, delimiter, prettyPrinter );
		}

		public PrettyPrinter parens( Consumer< PrettyPrinter > prettyPrinter ) {
			return surround( PrettyPrinter::lparen, PrettyPrinter::rparen, prettyPrinter );
		}

		public PrettyPrinter brackets( Consumer< PrettyPrinter > prettyPrinter ) {
			return surround( PrettyPrinter::lbrack, PrettyPrinter::rbrack, prettyPrinter );
		}

		public PrettyPrinter braces( Consumer< PrettyPrinter > prettyPrinter ) {
			return surround( PrettyPrinter::lbrace, PrettyPrinter::rbrace, prettyPrinter );
		}

		public PrettyPrinter spacedParens( Consumer< PrettyPrinter > prettyPrinter ) {
			return surround(
				PrettyPrinter::lparen,
				PrettyPrinter::rparen,
				_0 -> surround( PrettyPrinter::space, prettyPrinter ) );
		}

		public PrettyPrinter spacedBrackets( Consumer< PrettyPrinter > prettyPrinter ) {
			return surround(
				PrettyPrinter::lbrack,
				PrettyPrinter::rbrack,
				_0 -> surround( PrettyPrinter::space, prettyPrinter ) );
		}

		public PrettyPrinter nest( Consumer< PrettyPrinter > prettyPrinter ) {
			indentationLevel++;
			run( prettyPrinter );
			indentationLevel--;
			return this;
		}

		public PrettyPrinter newCodeBlock( Consumer< PrettyPrinter > prettyPrinter ) {
			/*
			 * return lbrace() .nest( () -> newline().run( prettyPrinter ) ) .newline() .rbrace();
			 */
			return braces( _1 -> nest( _2 -> newline().run( prettyPrinter ) ).newline() );
		}

		public < T > PrettyPrinter intercalate( Iterator< T > it,
			BiConsumer< T, PrettyPrinter > elementPrettyPrinter,
			Consumer< PrettyPrinter > interleave ) {
			while( it.hasNext() ) {
				elementPrettyPrinter.accept( it.next(), this );
				if( it.hasNext() ) {
					run( interleave );
				}
			}
			return this;
		}

		public < T > PrettyPrinter intercalate( Collection< T > collection,
			BiConsumer< T, PrettyPrinter > elementPrettyprinter,
			Consumer< PrettyPrinter > interleave ) {
			return intercalate( collection.iterator(), elementPrettyprinter, interleave );
		}

		public < T > PrettyPrinter ifPresent( Optional< T > optional, BiConsumer< T, PrettyPrinter > prettyPrinter ) {
			optional.ifPresent( t -> prettyPrinter.accept( t, this ) );
			return this;
		}


		public PrettyPrinter ifTrueOrElse( boolean condition,
			Consumer< PrettyPrinter > trueCase,
			Consumer< PrettyPrinter > falseCase ) {
			if( condition ) {
				trueCase.accept( this );
			} else {
				falseCase.accept( this );
			}
			return this;
		}

		public PrettyPrinter onlyIf( boolean condition, Consumer< PrettyPrinter > prettyPrinter ) {
			return ifTrueOrElse( condition, prettyPrinter, PrettyPrinter::empty );
		}

		public PrettyPrinter empty() {
			return this;
		}

		public PrettyPrinter space() {
			pp.append( ' ' );
			return this;
		}

		public PrettyPrinter spaces( int n ) {
			for( int i = 0; i < n; ++i ) {
				space();
			}
			return this;
		}

		public PrettyPrinter colon() {
			pp.append( ':' );
			return this;
		}

		public PrettyPrinter comma() {
			pp.append( ',' );
			return this;
		}

		public PrettyPrinter dot() {
			pp.append( '.' );
			return this;
		}

		public PrettyPrinter lparen() {
			pp.append( '(' );
			return this;
		}

		public PrettyPrinter rparen() {
			pp.append( ')' );
			return this;
		}

		public PrettyPrinter lbrack() {
			pp.append( '[' );
			return this;
		}

		public PrettyPrinter rbrack() {
			pp.append( ']' );
			return this;
		}

		public PrettyPrinter lbrace() {
			pp.append( '{' );
			return this;
		}

		public PrettyPrinter rbrace() {
			pp.append( '}' );
			return this;
		}
	}

	@Override
	public void visit(SolicitResponseExpressionNode n) {
		pp.append( n.id() )
			.append( "@" )
			.append( n.outputPortId() )
			.spacedParens( _0 -> _0
				.ifPresent( Optional.ofNullable( n.outputExpression() ),
					( outExpre, _1 ) -> outExpre.accept( this ) ) );
	}

	@Override
	public void visit(IfExpressionNode n) {
		pp.append("if")
			.spacedParens( _1 -> n.guard().accept(this));
		n.thenExpression().accept(this);
		pp.ifPresent(
			Optional.ofNullable( n.elseExpression() ),
			( proc, _0 ) -> _0
				.surround( PrettyPrinter::space, asPPConsumer( PrettyPrinter::append, "else" ) )
				.newCodeBlock( _1 -> proc.accept( this ) ) );
	}
}
