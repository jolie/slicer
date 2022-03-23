/*
 * Copyright (C) 2021 Valentino Picotti
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

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.*;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.*;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.util.Unit;

import java.util.*;

public class DependenciesResolver implements OLVisitor< Unit, Set< OLSyntaxNode > > {
	final Map< OLSyntaxNode, Set< OLSyntaxNode > > declDependencies = new HashMap<>();
	final Map< String, ImportStatement > importedSymbolsMap = new HashMap<>();

	DependenciesResolver( Program p ) {
		collectDeclarationsAndImportedSymbols( p );
		/* Compute dependencies */
		p.accept( this );
	}

	Set< OLSyntaxNode > getServiceDependencies( ServiceNode n ) {
		assert declDependencies.containsKey( n );
		assert declDependencies.get( n ) != null;
		return declDependencies.get( n );
	}

	private void collectDeclarationsAndImportedSymbols( Program program ) {
		for( OLSyntaxNode n : program.children() ) {
			if( n instanceof ImportStatement ) {
				ImportStatement is = (ImportStatement) n;
				ImportSymbolTarget[] importedSymbols = is.importSymbolTargets();
				for( ImportSymbolTarget ist : importedSymbols ) {
					importedSymbolsMap.put( ist.localSymbolName(), is );
				}
			} else {
				declDependencies.put( n, null );
			}
		}
	}

	@Override
	public Set< OLSyntaxNode > visit( Program n, Unit ctx ) {
		Set< OLSyntaxNode > dependencies = new HashSet<>();
		n.children().forEach( c -> dependencies.addAll( c.accept( this ) ) );
		return dependencies;
	}

	@Override
	public Set< OLSyntaxNode > visit( OneWayOperationDeclaration ow, Unit ctx ) {
		Set< OLSyntaxNode > result = new HashSet<>();
		/*
		 * If a type is a top level program declaration, we add it to the dependencies of the Operation
		 * Declaration, otherwise it is an imported symbol and the visitor will add it's import statement as
		 * dependency.
		 */
		if( declDependencies.containsKey( ow.requestType() ) ) {
			result.add( ow.requestType() );
		}
		result.addAll( ow.requestType().accept( this ) );
		return result;
	}

	@Override
	public Set< OLSyntaxNode > visit( RequestResponseOperationDeclaration rr, Unit ctx ) {
		Set< OLSyntaxNode > result = new HashSet<>();
		/*
		 * If a type is a top level program declaration, we add it to the dependencies of the
		 * OperationDeclaration, otherwise it is an imported symbol and the visitor will add it's import
		 * statement as dependency.
		 */
		if( declDependencies.containsKey( rr.requestType() ) ) {
			result.add( rr.requestType() );
		}
		if( declDependencies.containsKey( rr.responseType() ) ) {
			result.add( rr.responseType() );
		}
		result.addAll( rr.requestType().accept( this ) );
		result.addAll( rr.responseType().accept( this ) );
		return result;
	}

	@Override
	public Set< OLSyntaxNode > visit( DefinitionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ParallelStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( SequenceStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( NDChoiceStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( OneWayOperationStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( RequestResponseOperationStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( NotificationOperationStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( SolicitResponseOperationStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( LinkInStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( LinkOutStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( AssignStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( AddAssignStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( SubtractAssignStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( MultiplyAssignStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( DivideAssignStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( IfStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( DefinitionCallStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( WhileStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( OrConditionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( AndConditionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( NotExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( CompareConditionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ConstantIntegerExpression n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ConstantDoubleExpression n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ConstantBoolExpression n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ConstantLongExpression n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ConstantStringExpression n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ProductExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( SumExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( VariableExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( NullProcessStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( Scope n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( InstallStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( CompensateStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ThrowStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ExitStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ExecutionInfo n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( CorrelationSetInfo n, Unit ctx ) {
		return new HashSet<>();
	}

	public Set< OLSyntaxNode > visit( PortInfo n, Unit ctx ) {
		Set< OLSyntaxNode > result = new HashSet<>();
		n.getInterfaceList().forEach(
			iFace -> result.addAll( iFace.accept( this ) ) );
		return result;
	}

	@Override
	public Set< OLSyntaxNode > visit( InputPortInfo n, Unit ctx ) {
		return visit( (PortInfo) n, ctx );
	}

	@Override
	public Set< OLSyntaxNode > visit( OutputPortInfo n, Unit ctx ) {
		return visit( (PortInfo) n, ctx );
	}

	@Override
	public Set< OLSyntaxNode > visit( PointerStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( DeepCopyStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( RunStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( UndefStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ValueVectorSizeExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( PreIncrementStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( PostIncrementStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( PreDecrementStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( PostDecrementStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ForStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ForEachSubNodeStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ForEachArrayItemStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( SpawnStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( IsTypeExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( InstanceOfExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( TypeCastExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( SynchronizedStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( CurrentHandlerStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( EmbeddedServiceNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( InstallFixedVariableExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( VariablePathNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( TypeInlineDefinition tid, Unit ctx ) {
		if( declDependencies.get( tid ) != null ) { // Dependencies already computed
			return declDependencies.get( tid );
		}
		// Otherwise compute its dependencies by visiting the subtypes:
		Set< OLSyntaxNode > newDependencies = new HashSet<>();
		if( tid.subTypes() != null ) {
			tid.subTypes()
				.stream()
				.map( e -> e.getValue().accept( this ) )
				.forEach( newDependencies::addAll );
		}

		if ( declDependencies.containsKey( tid ) ) { // If tid is a top level declaration
			// Update its dependencies
			declDependencies.put( tid, newDependencies );
		}
		return newDependencies;
	}

	@Override
	public Set< OLSyntaxNode > visit( TypeDefinitionLink tdl, Unit ctx ) {
		if( declDependencies.get( tdl ) != null ) {
			return declDependencies.get( tdl );
		}
		Set< OLSyntaxNode > newDependencies = new HashSet<>();
		if( declDependencies.containsKey( tdl.linkedType() ) ) {
			newDependencies.add( tdl.linkedType() );
			newDependencies.addAll( tdl.linkedType().accept( this ) );
		} else if( importedSymbolsMap.containsKey( tdl.linkedTypeName() ) ) {
			newDependencies.add( importedSymbolsMap.get( tdl.linkedTypeName() ) );
		} else {
			// We end up here for the type definition type PAID : long
			// For which linkedType is null, linkedTypeName is the string "PAID"
			assert false;
		}
		declDependencies.put( tdl, newDependencies );
		return newDependencies;
	}

	@Override
	public Set< OLSyntaxNode > visit( TypeChoiceDefinition tcd, Unit ctx ) {
		if( declDependencies.get( tcd ) != null ) {
			return declDependencies.get( tcd );
		}
		// Otherwise compute its dependencies by visiting each alternative:
		Set< OLSyntaxNode > newDependencies = new HashSet<>();
		newDependencies.addAll( tcd.left().accept( this ) );
		newDependencies.addAll( tcd.right().accept( this ) );
		if ( declDependencies.containsKey( tcd ) ) { // If tcd is a top level declaration
			// Update its dependencies
			declDependencies.put( tcd, newDependencies );
		}
		return newDependencies;
	}

	@Override
	public Set< OLSyntaxNode > visit( InterfaceDefinition n, Unit ctx ) {
		/*
		 * We have to distinguish between interface definitions of a port declaration and actual interface
		 * definitions at the top level of the program.
		 */
		if( declDependencies.get( n ) != null ) {
			return declDependencies.get( n );
		}
		Set< OLSyntaxNode > newDependencies = new HashSet<>();
		if( declDependencies.containsKey( n ) ) { // The declaration is an actual interface declaration
			n.operationsMap().entrySet()
				.stream()
				.map( e -> e.getValue().accept( this ) )
				.forEach( newDependencies::addAll );
			declDependencies.put( n, newDependencies );
		} else { // The interface definition is an interface appearing in a Port declaration.
			if( importedSymbolsMap.containsKey( n.name() ) ) { // The interface is an imported symbol
				newDependencies.add( importedSymbolsMap.get( n.name() ) );
			} else { // The interface is not imported, find the actual definition in this program
				InterfaceDefinition actualDefinition = null;
				for( OLSyntaxNode decl : declDependencies.keySet() ) {
					if( decl instanceof InterfaceDefinition
						&& ((InterfaceDefinition) decl).name().equals( n.name() ) ) {
						actualDefinition = (InterfaceDefinition) decl;
						break;
					}
				}
				assert actualDefinition != null;
				// The actual definition is a dependency of the Port declaration
				newDependencies.add( actualDefinition );
				newDependencies.addAll( actualDefinition.accept( this ) );
			}
		}
		return newDependencies;
	}

	@Override
	public Set< OLSyntaxNode > visit( DocumentationComment n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( FreshValueExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( CourierDefinitionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( CourierChoiceStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( NotificationForwardStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( SolicitResponseForwardStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( InterfaceExtenderDefinition n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( InlineTreeExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( VoidExpressionNode n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ProvideUntilStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ImportStatement n, Unit ctx ) {
		return new HashSet<>();
	}

	@Override
	public Set< OLSyntaxNode > visit( ServiceNode n, Unit ctx ) {
		if( declDependencies.get( n ) != null ) {
			return declDependencies.get( n );
		}
		Set< OLSyntaxNode > newDependencies = new HashSet<>( n.program().accept( this ) );
		declDependencies.put( n, newDependencies );
		return newDependencies;
	}

	@Override
	public Set< OLSyntaxNode > visit( EmbedServiceNode n, Unit ctx ) {
		if( importedSymbolsMap.containsKey( n.serviceName() ) ) {
			return new HashSet<>( Arrays.asList( importedSymbolsMap.get( n.serviceName() ) ) );
		} else {
			// The service name is not imported. It refers to a ServiceNode declared in this program
			assert declDependencies.containsKey( n.service() );
			Set< OLSyntaxNode > dependencies = new HashSet<>( n.service().accept( this ) );
			dependencies.add( n.service() );
			return dependencies;
		}
	}
}
